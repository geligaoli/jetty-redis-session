//package cn.embedsoft.jetty.session.redis;
//
//import org.eclipse.jetty.server.session.SessionData;
//
//import com.esotericsoftware.kryo.Kryo;
//import com.esotericsoftware.kryo.Serializer;
//import com.esotericsoftware.kryo.io.Input;
//import com.esotericsoftware.kryo.io.Output;
//
//public class SessionDataSerializer extends Serializer<SessionData> {
//
//    @Override
//    public void write(Kryo kryo, Output output, SessionData object) {
//        kryo.writeObject(output, object);
//    }
//
//    @Override
//    public SessionData read(Kryo kryo, Input input, Class<SessionData> type) {
//        SessionData data = new SessionData();
//        
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//}
