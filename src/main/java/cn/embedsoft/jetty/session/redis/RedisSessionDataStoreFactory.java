package cn.embedsoft.jetty.session.redis;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * redis session datastore factory
 * 
 * @author gyli@embed-soft.cn
 *
 */
public class RedisSessionDataStoreFactory extends AbstractSessionDataStoreFactory {
    private RedisConfig config;

    public void setConfig(RedisConfig config) {
        this.config = config;
    }

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception {
        RedisSessionDataStore ds = new RedisSessionDataStore(config);
        ds.setGracePeriodSec(getGracePeriodSec());
        ds.setSavePeriodSec(getSavePeriodSec());
        return ds;
    }
    
}
