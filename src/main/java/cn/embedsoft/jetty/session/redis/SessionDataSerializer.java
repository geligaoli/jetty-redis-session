package cn.embedsoft.jetty.session.redis;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jetty.server.session.SessionData;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class SessionDataSerializer extends Serializer<SessionData> {

    @Override
    public void write(Kryo kryo, Output output, SessionData session) {
        output.writeString(session.getId());
        output.writeString(session.getContextPath());
        output.writeString(session.getVhost());
        output.writeString(session.getLastNode());
        
        long created = session.getCreated();
        output.writeVarLong(created, true);
        output.writeVarLong(session.getAccessed() == 0 ? 0 : (session.getAccessed() - created), true);
        output.writeVarLong(session.getLastAccessed() == 0 ? 0 : (session.getLastAccessed() - created), true);
        output.writeVarLong(session.getCookieSet() == 0 ? 0 : (session.getCookieSet() - created), true);
        output.writeVarLong(session.getExpiry() == 0 ? 0 : (session.getExpiry() - created), true);
        output.writeVarLong(session.getMaxInactiveMs(), false);
        output.writeVarLong(session.getLastSaved() == 0 ? 0 : (session.getLastSaved() - created), true);
        
        Set<String> keys = session.getKeys();
        output.writeVarInt(keys.size(), true);
        
        for (String name : keys) {
            output.writeString(name);
            kryo.writeClassAndObject(output, session.getAttribute(name));
        }
    }

    @Override
    public SessionData read(Kryo kryo, Input input, Class<SessionData> type) {

        String id = input.readString();
        String contextPath = input.readString();
        String vhost = input.readString();
        String lastNode = input.readString();
        
        long created = input.readVarLong(true);
        long accessed = input.readVarLong(true);
        long lastAccessed = input.readVarLong(true);
        long cookieSet = input.readVarLong(true);
        long expiry = input.readVarLong(true);
        long maxIdle = input.readVarLong(false);
        long lastSaved = input.readVarLong(true);
        
        accessed = accessed == 0 ? 0 : accessed + created;
        lastAccessed = lastAccessed == 0 ? 0 : lastAccessed + created;
        cookieSet = cookieSet == 0 ? 0 : cookieSet + created;
        expiry = expiry == 0 ? 0 : expiry + created;
        lastSaved = lastSaved == 0 ? 0 : lastSaved + created;

        SessionData data = new SessionData(id, contextPath, vhost, created, accessed, lastAccessed, maxIdle); 
        data.setContextPath(contextPath);
        data.setVhost(vhost);
        
        data.setLastNode(lastNode);
        data.setCookieSet(cookieSet);
        data.setExpiry(expiry);
        data.setMaxInactiveMs(maxIdle);
        data.setLastSaved(lastSaved);

        // Session Attributes
        int size = input.readVarInt(true);
        if (size > 0) {
            Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
            for (int i=0; i<size; i++) {
                String key = input.readString();
                attributes.put(key, kryo.readClassAndObject(input));
            }
            data.putAllAttributes(attributes);
        }
        return data;
    }

}
