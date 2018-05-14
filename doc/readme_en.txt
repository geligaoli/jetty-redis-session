Functions:

    Store jetty session to redis and read from it.    

Usage:

    Copy the two files in the resources directory to the jetty directory. like this.
    
    ${jetty.home}/etc/sessions/session-store-embedsoft-redis.xml
    ${jetty.home}/modules/session-store-embedsoft-redis.mod
    
    Copy jedis-2.9.0.jar commons-pool2-2.5.0.jar jetty-redis-sessions-0.1.0.jar to ${jetty.home}/lib/ext/ directory
    
    Set the jetty.base variable and execute "java -jar /usr/local/jetty/start.jar --add-to-start=session-store-embedsoft-redis".
    
    In the ${jetty.base}/start.d directory, the session-store-embedsoft-redis.ini configuration file is generated.
    
    
Config:
    
    The contents of the session-store-embedsoft-redis.ini file
    
    # Use uri format to define redis access. The available setting are:
    # redis : [: password@] host [: port] [/ database] [? [timeout=timeout] [&clientName=clientName]]
    # rediss : [: password@] host [: port] [/ database] [? [timeout=timeout] [&clientName=clientName]]
    # redis-sentinel :// [: password@] host1[: port1] [, host2[: port2]] [, hostN[: portN]] [/ database][?[timeout=timeout] [&sentinelMasterId=sentinelMasterId] [ &clientName=clientName]]
    # E.g:
    Jetty.session.embedsoft.redis.uri=redis://127.0.0.1:6379/0?timeout=2000
    
    # Maximum number of connection pool connections
    jetty.session.embedsoft.redis.pool.maxTotal=8
    
    # Maximum number of idle connections in the pool
    jetty.session.embedsoft.redis.pool.maxIdle=4
    
    # Minimum number of free connections in the pool
    jetty.session.embedsoft.redis.pool.minIdle=0
    
    # Waiting time (ms) while the connection pool is full, -1 waiting for ever.
    jetty.session.embedsoft.redis.pool.maxWaitMillis=-1
    
    # The cache time of the local session of the jetty (seconds)
    # The default value of 0 means that every time the page is refreshed, the session data is read from redis and written back to redis.
    # Other values, if jetty does not have the session data, or since the last time read redis, more than "savePeriodSec" seconds, then read and write sessions from redis.
    #
    # This setting can greatly reduce read and write traffic of redis. If you use session sticky, you can consider increasing this value.
    jetty.session.embedsoft.redis.savePeriodSec=0

