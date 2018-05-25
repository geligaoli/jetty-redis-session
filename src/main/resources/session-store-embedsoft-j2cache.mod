DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Enables session data store in a j2cache

[tags]
session

[provides]
session-store

[depend]
sessions

[files]

[xml]
etc/sessions/session-store-embedsoft-j2cache.xml

[lib]
lib/ext/jetty-j2cache-sessions-0.1.0.jar
lib/ext/commons-pool2-2.5.0.jar
lib/ext/jedis-2.9.0.jar

[license]
GPL 3.0

[ini-template]
jetty.session.embedsoft.j2cache.configfile=
jetty.session.embedsoft.j2cache.savePeriodSec=0
