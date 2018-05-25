package cn.embedsoft.jetty.redis.session;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import cn.embedsoft.jetty.session.j2cache.J2CacheConfig;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.util.JedisURIHelper;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {

        
        assertTrue( true );
    }
}
