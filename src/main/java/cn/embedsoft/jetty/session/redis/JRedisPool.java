package cn.embedsoft.jetty.session.redis;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.Pool;

public class JRedisPool {
    
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    

    public static Pool<Jedis> create(RedisConfig config) {
        
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
            
            return new JedisSentinelPool(sentinelMasterId, sentinels, poolConfig, timeout, password, database, clientName);
            
        } else {
            boolean ssl = "rediss".equals(uri.getScheme());
            String password = JedisURIHelper.getPassword(uri);
            String host = uri.getHost();
            int port = uri.getPort() < 0 ? DEFAULT_PORT : uri.getPort();
            
            return new JedisPool(poolConfig, host, port, timeout, password, database, clientName, ssl, null, null, null);
        }
        
    }

}
