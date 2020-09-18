package cn.embedsoft.jetty.session.j2cache;

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

import net.oschina.j2cache.J2Cache;

/**
 * J2Cache store of session data.
 * 
 * @author gyli@embed-soft.cn
 *
 */
@ManagedObject
public class J2CacheSessionDataStore extends AbstractSessionDataStore {
    private static final Logger LOG = Log.getLogger(J2CacheSessionDataStore.class);
    // j2cache建议是在配置文件中，设置超时时间
    // private static final int DEFAULT_CACHE_TIMEOUT = 3600;

    protected J2CacheConfig config;
    private String _contextString;

    public J2CacheSessionDataStore(J2CacheConfig config) {
        super();
        this.config = config;
    }

    private void initializeJ2Cache() {
        if (J2CacheKit.getCacheChannel() == null) {
            J2CacheKit.init(J2Cache.getChannel(), "JETTY_SESSIONS");

            if (config.getSavePeriodSec() > 0)
                this.setSavePeriodSec(config.getSavePeriodSec());
        }
    }

    private String getIdWithContext(String id) {
        return _contextString + "_" + id;
    }

    @Override
    public void initialize(SessionContext context) throws Exception {
        super.initialize(context);
        _contextString = _context.getCanonicalContextPath() + "_" + _context.getVhost();
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
            public void run() {
                try {
                    String keydata = getIdWithContext(id);
                    if (J2CacheKit.exists(keydata)) {
                        SessionData data = J2CacheKit.get(keydata);
                        reference.set(data.getExpiry() <= 0 || data.getExpiry() > System.currentTimeMillis());
                    }
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        };

        // ensure this runs with the context classloader set
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
            public void run() {
                LOG.debug("Loading SessionID {} from J2Cache", id);
                try {
                    String keydata = getIdWithContext(id);
                    SessionData data = J2CacheKit.get(keydata);
                    reference.set(data);
                } catch (Exception e) {
                    exception.set(e);
                }
            }
        };

        // ensure this runs with the context classloader set
        _context.run(r);

        if (exception.get() != null)
            throw exception.get();

        return reference.get();
    }

    @Override
    public boolean delete(String id) throws Exception {
        LOG.debug("Deleting SessionID {} from J2Cache", id);
        J2CacheKit.remove(getIdWithContext(id));
        return true;
    }

    @Override
    public void doStore(String id, SessionData session, long lastSaveTime) throws Exception {
        if (LOG.isDebugEnabled())
            LOG.debug("Store session: {} to J2Cache", session.toString());

        // int cachetimeout = Math.max(DEFAULT_CACHE_TIMEOUT,
        // (int)(session.getMaxInactiveMs() / 900) + this.getSavePeriodSec() *
        // 2);
        J2CacheKit.put(getIdWithContext(id), session);
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

                // if the session no longer exists
                if (sd == null) {
                    expired.add(candidate);
                    LOG.debug("SessionID {} does not exist in J2Cache", candidate);
                } else {
                    if (_context.getWorkerName().equals(sd.getLastNode())) {
                        // we are its manager, add it to the expired set if it
                        // is expired now
                        if ((sd.getExpiry() > 0) && sd.getExpiry() <= now) {
                            expired.add(candidate);
                            LOG.debug("SessionID {} managed by {} is expired", candidate, _context.getWorkerName());
                        }
                    } else {
                        /**
                         * at least 1 graceperiod since the last expiry check.
                         * If we haven't done previous expiry checks, then check
                         * those that have expired at least 3 graceperiod ago.
                         */
                        if ((sd.getExpiry() > 0) && sd.getExpiry() < (now
                                - TimeUnit.SECONDS.toMillis((_lastExpiryCheckTime <= 0 ? 3 : 1) * _gracePeriodSec)))
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
        if (config == null || config.getConfigfile() == null)
            throw new IllegalStateException("No J2Cache config");

        this.initializeJ2Cache();

        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    @Override
    public SessionData doLoad(String id) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
