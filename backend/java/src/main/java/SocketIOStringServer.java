import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

/**
 * 与{@link SocketIOProtoServer}类似，这里为了测试就传递简单的String类型
 */
public class SocketIOStringServer implements ConnectListener, DisconnectListener{
	
	private static final String HOST = "localhost";
	private static final int PORT = 3001;
	
	private final SocketIOServer server;
	
	public SocketIOStringServer(){
        server = new SocketIOServer(config());
	}
	
	private Configuration config(){
	    Configuration config = new Configuration();
        config.setHostname(HOST);
        config.setPort(PORT);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        return config;
	}
	
	public void start(){
        server.addConnectListener(this);
        server.addDisconnectListener(this);
        
        server.addEventListener("message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
            	Message message = new Message(data);
            	System.out.println("Received: "+message.getText());
                // Transform the text to upper case
            	message.setText(message.getText().toUpperCase());
                // Re-encode it and send it back
            	client.sendEvent("message", message.getText());
                System.out.println("Sent: "+message.getText());
            }
        });
        
        server.start();
        System.out.println("\n------ "+this.getClass().getSimpleName()+"start on "+PORT+" ------\n");
	}
	
	public void stop(){
	    server.stop();
	}

	@Override
	public void onConnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" connecting..."+server.getAllClients().size());
	}
	
	@Override
	public void onDisconnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" disconnecting..."+server.getAllClients().size());
	}
	
	public static void main(String[] args){
    	new SocketIOStringServer().start();
    }

}
