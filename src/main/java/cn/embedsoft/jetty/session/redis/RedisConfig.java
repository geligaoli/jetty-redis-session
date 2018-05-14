package cn.embedsoft.jetty.session.redis;

/**
 * redis config
 * 
 * @author gyli@embed-soft.cn
 *
 */
public class RedisConfig {
    /**
     * redis :// [: password@] host [: port] [/ database][? [timeout=timeout] [&clientName=clientName]]
     * rediss :// [: password@] host [: port] [/ database][? [timeout=timeout] [&clientName=clientName]]
     * redis-sentinel :// [: password@] host1[: port1] [, host2[: port2]] [, hostN[: portN]] [/ database][?[timeout=timeout] [&sentinelMasterId=sentinelMasterId] [&clientName=clientName]]
     */
    private String uri;
    private int maxTotal = 8;
    private int maxIdle = 4;
    private int minIdle = 0;
    private long maxWaitMillis = -1L;
    private int savePeriodSec = 0;
    
    public String getUri() {
        return uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public int getMaxTotal() {
        return maxTotal;
    }
    
    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }
    
    public int getMaxIdle() {
        return maxIdle;
    }
    
    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }
    
    public int getMinIdle() {
        return minIdle;
    }
    
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }
    
    public long getMaxWaitMillis() {
        return maxWaitMillis;
    }
    
    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public int getSavePeriodSec() {
        return savePeriodSec;
    }

    public void setSavePeriodSec(int savePeriodSec) {
        this.savePeriodSec = savePeriodSec;
    }

    
}
