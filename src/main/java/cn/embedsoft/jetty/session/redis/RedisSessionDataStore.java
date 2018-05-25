package cn.embedsoft.jetty.session.redis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionContext;
import org.eclipse.jetty.server.session.SessionData;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.ManagedObject;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * redis store of session data.
 * 
 * @author gyli@embed-soft.cn
 *
 */
@ManagedObject
public class RedisSessionDataStore extends AbstractSessionDataStore{
    private static final Logger LOG = Log.getLogger(RedisSessionDataStore.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int REDIS_CACHE_TIMEOUT = 3600;
    static FSTConfiguration conf = FSTConfiguration.createUnsafeBinaryConfiguration();
    
    protected RedisConfig config;
    protected Pool<Jedis> redisPool;

    private String _contextString;
    
    public RedisSessionDataStore(RedisConfig config) {
        super();
        this.config = config;
    }
    
    private void initializeRedisPool() {
        if (redisPool == null) {
            redisPool = JRedisPool.create(this.config);
            
            if (config.getSavePeriodSec() > 0)
                this.setSavePeriodSec(config.getSavePeriodSec());
        }
    }
    
    private byte[] getIdWithContext(String id) {
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
                            try (ByteArrayInputStream bin = new ByteArrayInputStream(bdata)) {
                                conf.setClassLoader(_context.getContext().getClassLoader());
                                FSTObjectInput fin = conf.getObjectInput(bin);
                                SessionData data = (SessionData) fin.readObject();
                                reference.set(data.getExpiry() <= 0 || data.getExpiry() > System.currentTimeMillis());
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
                        try (ByteArrayInputStream bin = new ByteArrayInputStream(bdata)) {
                            conf.setClassLoader(_context.getContext().getClassLoader());
                            FSTObjectInput fin = conf.getObjectInput(bin);
                            SessionData data = (SessionData) fin.readObject();
                            reference.set(data);
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
      try (ByteArrayOutputStream bot = new ByteArrayOutputStream(1400)) {
          
          if(LOG.isDebugEnabled())
              LOG.debug("Store session: {} to Redis", session.toString());
          
          FSTObjectOutput fout = conf.getObjectOutput(bot);
          fout.writeObject(session);
          fout.flush();
          
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
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        if (redisPool != null) {
            redisPool.close();
            redisPool = null;
        }
        super.doStop();
    }

}
