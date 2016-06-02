import com.corundumstudio.socketio.store.pubsub.PubSubMessage;

public class BroadcastMessage extends PubSubMessage {

    private static final long serialVersionUID = 5641188272650134025L;
    
    private byte[] data;

    public BroadcastMessage() {
    }

    public BroadcastMessage(byte[] data) {
        super();
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
    
}
