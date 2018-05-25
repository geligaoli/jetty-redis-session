
功能：

    将jetty的session，存放j2cache。以便于支持分布式。
    

使用：

    将 resources 目录下的两个文件，拷贝到jetty的目录下。
    
    ${jetty.home}/etc/sessions/session-store-embedsoft-redis.xml
    ${jetty.home}/modules/session-store-embedsoft-redis.mod
    
    将 jedis-2.9.0.jar commons-pool2-2.5.0.jar jetty-j2cache-sessions-0.1.0.jar 拷贝到 ${jetty.home}/lib/ext/ 目录下
    
    设置 jetty.base 变量，然后执行  java -jar /usr/local/jetty/start.jar --add-to-start=session-store-embedsoft-redis
    
    在 ${jetty.base}/start.d目录下，会生成 session-store-embedsoft-redis.ini 配置文件。
    
    
配置：
    
    session-store-embedsoft-redis.ini 文件的内容
    
    # 采用uri格式来定义redis的访问。可用的方式有：
    # redis :// [: password@] host [: port] [/ database][? [timeout=timeout] [&clientName=clientName]]
    # rediss :// [: password@] host [: port] [/ database][? [timeout=timeout] [&clientName=clientName]]
    # redis-sentinel :// [: password@] host1[: port1] [, host2[: port2]] [, hostN[: portN]] [/ database][?[timeout=timeout] [&sentinelMasterId=sentinelMasterId] [&clientName=clientName]]
    # 例如：
    jetty.session.embedsoft.redis.uri=redis://127.0.0.1:6379/0?timeout=2000
    
    # 连接池的最大连接数
    jetty.session.embedsoft.redis.pool.maxTotal=8
    
    # 连接池的最大空闲数
    jetty.session.embedsoft.redis.pool.maxIdle=4
    
    # 连接池的最小空闲数
    jetty.session.embedsoft.redis.pool.minIdle=0
    
    # 连接池满后的等待时间， -1 一直等待
    jetty.session.embedsoft.redis.pool.maxWaitMillis=-1
    
    # jetty的本地session的缓存时间（秒）
    # 默认值 0 表示每次页面刷新，都从redis中读session数据，页面完成再写回redis
    # 其他值，当jetty没有该session数据，或者自上次读redis时间，超过多少秒之后，再从redis读写session。
    #
    # 该设置可以大大减少redis的读写流量，如果使用了session sticky，可以考虑增加该值。
    jetty.session.embedsoft.redis.savePeriodSec=0
        


