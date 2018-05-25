package cn.embedsoft.jetty.session.j2cache;

import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * j2cache session datastore factory
 * 
 * @author gyli@embed-soft.cn
 *
 */
public class J2CacheSessionDataStoreFactory extends AbstractSessionDataStoreFactory {
    private static final Logger LOG = Log.getLogger(J2CacheSessionDataStoreFactory.class);
    private J2CacheConfig config;

    public void setConfig(J2CacheConfig config) {
        this.config = config;
    }

    @Override
    public SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception {
        LOG.info("J2CacheSessionDataStoreFactory getSessionDataStore; {} {}", 
                handler.getServer().getURI().toString(), handler.getSessionPath());
        
        J2CacheSessionDataStore ds = new J2CacheSessionDataStore(config);
        ds.setGracePeriodSec(getGracePeriodSec());
        ds.setSavePeriodSec(getSavePeriodSec());
        return ds;
    }
    
}
