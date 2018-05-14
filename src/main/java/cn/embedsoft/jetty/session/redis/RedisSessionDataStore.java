package cn.embedsoft.jetty.session.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoPool;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.JedisURIHelper;
import redis.clients.util.Pool;

/**
 * redis store of session data.
 * 
 * @author gyli@embed-soft.cn
 *
 */
@ManagedObject
public class RedisSessionDataStore extends AbstractSessionDataStore{
    private  final static Logger LOG = Log.getLogger(RedisSessionDataStore.class);
    
    public static final Charset UTF8 = Charset.forName("UTF-8");
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int REDIS_CACHE_TIMEOUT = 3600;
    
    protected RedisConfig config;
    protected Pool<Jedis> redisPool;
    protected KryoPool kryoPool;

    private String _contextString;
    
    public RedisSessionDataStore(RedisConfig config) {
        super();
        this.config = config;
    }
    
    private void initializeKryoPool() {
        if (kryoPool == null) {
            // Build pool with SoftReferences enabled (optional)
            kryoPool = new KryoPool.Builder(new KryoFactory()).softReferences().build();
        }
    }
    
    private void initializeRedisPool() {
        if (redisPool == null) {
            URI uri = URI.create(config.getUri());
            if (! JedisURIHelper.isValid(uri))
                throw new IllegalStateException("jetty.session.embedsoft.redis.uri setting error");
            
            MultiMap<String> params = new MultiMap<String>();
            UrlEncoded.decodeUtf8To(uri.getQuery(), params);
            
            int database = JedisURIHelper.getDBIndex(uri);
            String clientName = params.getString("clientName");
            String _timeout = params.getString("timeout");
            int timeout = _timeout == null ? DEFAULT_TIMEOUT : Integer.parseInt(_timeout);
            
            GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
            poolConfig.setMaxTotal(config.getMaxTotal());
            poolConfig.setMaxIdle(config.getMaxIdle());
            poolConfig.setMinIdle(config.getMinIdle());
            poolConfig.setMaxWaitMillis(config.getMaxWaitMillis());
            
            if (config.getSavePeriodSec() > 0)
                this.setSavePeriodSec(config.getSavePeriodSec());
            
            // parse uri
            if ("redis-sentinel".equals(uri.getScheme())) {
                String sentinelMasterId = params.getString("sentinelMasterId");
                if (sentinelMasterId == null) sentinelMasterId = "master";
                
                String authority = uri.getAuthority();
                int pos = authority.indexOf('@');
                
                String password = pos <= 1 ? null : authority.substring(0, pos).split(":", 2)[1];
                String[] hosts = authority.substring(pos + 1).split(",");

                Set<String> sentinels = new HashSet<>(); 
                for(String host : hosts) {
                    if (host.isEmpty())
                        host = "127.0.0.1";
                    else if (host.charAt(0) == '[') {    // ipv6
                        sentinels.add(host.indexOf("]:") != -1 ? host : (host+":"+DEFAULT_SENTINEL_PORT));
                    } else
                        sentinels.add(host.indexOf(":") != -1 ? host : (host+":"+DEFAULT_SENTINEL_PORT));
                }
                
                redisPool = new JedisSentinelPool(sentinelMasterId, sentinels, poolConfig, timeout, password, database, clientName);
                
            } else {
                boolean ssl = "rediss".equals(uri.getScheme());
                String password = JedisURIHelper.getPassword(uri);
                String host = uri.getHost();
                int port = uri.getPort() < 0 ? DEFAULT_PORT : uri.getPort();
                
                redisPool = new JedisPool(poolConfig, host, port, timeout, password, database, clientName, ssl, null, null, null);
            }
        }
    }
    
    private byte[] getIdWithContext(String id) {
        // LOG.debug("redis key sessionid : {}_{}", _contextString, id);
        return (_contextString+"_"+id).getBytes(UTF8);
    }

    @Override
    public void initialize(SessionContext context) throws Exception
    {
        super.initialize(context);
        _contextString = _context.getCanonicalContextPath()+"_"+_context.getVhost();
    }
    
    @ManagedAttribute(value = "does store serialize sessions", readonly = true)
    @Override
    public boolean isPassivating() {
        return true;
    }

    @Override
    public boolean exists(String id) throws Exception {
        final AtomicReference<Boolean> reference = new AtomicReference<Boolean>(false);
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();

        Runnable r = new Runnable() {
            @Override
            public void run () {
                try (Jedis _client = redisPool.getResource()) {
                    byte[] keydata = getIdWithContext(id);
                    if (_client.exists(keydata)) {
                        byte[] bdata = _client.get(keydata);
                        if (bdata != null && bdata.length > 0) {
                            try (ByteArrayInputStream bin = new ByteArrayInputStream(bdata);
                                    Input input = new Input(bin)) {
                                
                                SessionData data = kryoPool.run(new KryoCallback<SessionData>() {
                                    @Override
                                    public SessionData execute(Kryo kryo) {
                                        kryo.setClassLoader(_context.getContext().getClassLoader());
                                        return (SessionData) kryo.readClassAndObject(input);
                                    }
                                });
                                
                                reference.set(data.getExpiry() <= 0 || data.getExpiry() > System.currentTimeMillis());
                                
                            } catch (Exception e1) {
                                _client.del(bdata);
                                exception.set(e1);
                            }
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        };
        
        //ensure this runs with the context classloader set
        _context.run(r);
        
        if (exception.get() != null)
            throw exception.get();
        
        return reference.get();
    }

    @Override
    public SessionData load(String id) throws Exception {
        final AtomicReference<SessionData> reference = new AtomicReference<SessionData>();
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();

        Runnable r = new Runnable() {
            @Override
            public void run () {
                LOG.debug("Loading SessionID {} from Redis", id);
                try (Jedis _client = redisPool.getResource()) {
                    byte[] bdata = _client.get(getIdWithContext(id));
                    
                    if (bdata != null && bdata.length > 0) {
                        try (ByteArrayInputStream bin = new ByteArrayInputStream(bdata);
                                Input input = new Input(bin)) {
                            
                            SessionData data = kryoPool.run(new KryoCallback<SessionData>() {
                                @Override
                                public SessionData execute(Kryo kryo) {
                                    kryo.setClassLoader(_context.getContext().getClassLoader());
                                    return (SessionData) kryo.readClassAndObject(input);
                                }
                            });
                            
                            reference.set(data);
                        } catch (Exception e1) {
                            _client.del(bdata);
                            exception.set(e1);
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        };
        
        //ensure this runs with the context classloader set
        _context.run(r);
        
        if (exception.get() != null)
            throw exception.get();
        
        return reference.get();
    }

    @Override
    public boolean delete(String id) throws Exception {
        LOG.debug("Deleting SessionID {} from Redis", id);
        try (Jedis _client = redisPool.getResource()) {
            _client.del(getIdWithContext(id));
        }
        return true;
    }

  @Override
  public void doStore(String id, SessionData session, long lastSaveTime) throws Exception {
      try (ByteArrayOutputStream bot = new ByteArrayOutputStream(1400);
              Output output = new Output(bot)) {
          
          if(LOG.isDebugEnabled())
              LOG.debug("Store session: {} to Redis", session.toString());
          
          kryoPool.run(new KryoCallback<SessionData>() {
              @Override
              public SessionData execute(Kryo kryo) {
                  //kryo.setClassLoader(_context.getContext().getClassLoader());
                  kryo.writeClassAndObject(output, session);
                  output.flush();
                  return session;
              }
          });
          
          try (Jedis _client = redisPool.getResource()) {
              int cachetimeout = Math.max(REDIS_CACHE_TIMEOUT,
                      (int)(session.getMaxInactiveMs() / 900) + this.getSavePeriodSec() * 2);
              _client.setex(getIdWithContext(id), cachetimeout, bot.toByteArray());
              //_client.set(getIdWithContext(id), bot.toByteArray()); // Never timeout
          }
      }
  }
    
    @Override
    public Set<String> doGetExpired(Set<String> candidates) {
        if (candidates == null || candidates.isEmpty())
            return candidates;

        long now = System.currentTimeMillis();

        Set<String> expired = new HashSet<>();

        for (String candidate : candidates) {
            // LOG.debug("Checking expiry for SessionID {}", candidate);
            try {
                SessionData sd = load(candidate);

                //if the session no longer exists
                if (sd == null) {
                    expired.add(candidate);
                    LOG.debug("SessionID {} does not exist in Redis", candidate);
                } else {
                    if (_context.getWorkerName().equals(sd.getLastNode())) {
                        //we are its manager, add it to the expired set if it is expired now
                        if ((sd.getExpiry() > 0) && sd.getExpiry() <= now) {
                            expired.add(candidate);
                            LOG.debug("SessionID {} managed by {} is expired", candidate, _context.getWorkerName());
                        }
                    } else {
                        /**
                         * at least 1 graceperiod since the last expiry check. If we haven't done previous expiry checks, then check
                         * those that have expired at least 3 graceperiod ago.
                         */
                        if ((sd.getExpiry() > 0) && sd.getExpiry() < (now - 
                                TimeUnit.SECONDS.toMillis((_lastExpiryCheckTime <= 0 ? 3 : 1) * _gracePeriodSec)))
                            expired.add(candidate);
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error checking if SessionID {} is expired. {}", candidate, e.getMessage());
            }
        }

        return expired;
    }

    @Override
    protected void doStart() throws Exception {
        if (config == null || config.getUri() == null)
            throw new IllegalStateException("No redis config");

        this.initializeRedisPool();
        this.initializeKryoPool();
        
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (redisPool != null) {
            redisPool.close();
            redisPool = null;
        }
        if (kryoPool != null) {
            kryoPool = null;
        }
        super.doStop();
    }

}
