import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.store.pubsub.ConnectMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;

public class MessageAnnotation {

    private final PubSubStore pubSubStore;
    
    public MessageAnnotation(PubSubStore pubSubStore){
        this.pubSubStore = pubSubStore;
    }
    
    @OnConnect
    public void onConnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        client.set("sessionId", sessionId);
        System.out.println(sessionId+" connecting..."+client.get("sessionId"));
//      RTopic<ConnectMessage> topic = redisson.getTopic("connected");
//      topic.publish(new ConnectMessage(client.getSessionId()));
//      pubSubStore.publish("connected", new ConnectMessage(client.getSessionId()));
        
        pubSubStore.publish("connected", new ConnectMessage(client.getSessionId()));
    }
    
    @OnEvent("message")
    public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
        Message message = Message.parse(data);
        System.out.println("Received: "+message.getText());
        // Transform the text to upper case
        message.setText(message.getText().toUpperCase());
        // Re-encode it and send it back
        client.sendEvent("message", message.toByteArray());
        System.out.println("Sent: "+message.getText());
    }
    
    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        System.out.println(sessionId+" disconnecting..."+client.get("sessionId"));
        client.del("sessionId");
    }
    
}
