DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Enables session data store in a redis

[tags]
session

[provides]
session-store

[depend]
sessions

[files]

[xml]
etc/sessions/session-store-embedsoft-redis.xml

[lib]
lib/ext/jetty-redis-sessions-0.1.0.jar
lib/ext/commons-pool2-2.5.0.jar
lib/ext/jedis-2.9.0.jar

[license]
GPL 3.0

[ini-template]
#jetty.session.embedsoft.redis.uri=redis://:password@host:port/database?timeout=timeout&clientName=clientName
jetty.session.embedsoft.redis.pool.maxTotal=8
jetty.session.embedsoft.redis.pool.maxIdle=4
jetty.session.embedsoft.redis.pool.minIdle=0
jetty.session.embedsoft.redis.pool.maxWaitMillis=-1
jetty.session.embedsoft.redis.savePeriodSec=0
