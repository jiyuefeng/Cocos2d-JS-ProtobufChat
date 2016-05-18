import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

/**
 * 对应NodeJS用SocketIO实现Protobuf接收/发送二进制的例子<br/>
 * https://github.com/whg333/protobuf.js/tree/master/examples/socketio
 */
public class BroadcastSocketIOServer implements ConnectListener, DisconnectListener{
	
	private static final String HOST = "localhost";
	private static final int PORT = 3001;
	
	private final SocketIOServer server;
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<Message>();
	
	public BroadcastSocketIOServer(){
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
        
        server.addEventListener("message", byte[].class, new DataListener<byte[]>() {
            @Override
            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
            	Message message = Message.parse(data);
            	System.out.println("Received: "+message.getText());
                // Transform the text to upper case
            	message.setText(message.getText().toUpperCase());
                // Re-encode it and send it back
            	client.sendEvent("message", message.toByteArray());
                System.out.println("Sent: "+message.getText());
            }
        });
        
        server.start();
        System.out.println("\n------ "+this.getClass().getSimpleName()+"start on "+PORT+" ------\n");

//        try {
//			Thread.sleep(Integer.MAX_VALUE);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//        server.stop();
        
        executor.scheduleAtFixedRate(new BroadcastMessageTask(this), TimeUnit.SECONDS.toMillis(2), 
        		TimeUnit.MILLISECONDS.toMillis(100), TimeUnit.MILLISECONDS);
        
        executor.scheduleAtFixedRate(new BroadcastGeneratorTask(this), TimeUnit.SECONDS.toMillis(3), 
        		TimeUnit.SECONDS.toMillis(2), TimeUnit.MILLISECONDS);
	}
	
	public void stop(){
	    server.stop();
	}

	@Override
	public void onConnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" connecting...");
	}
	
	@Override
	public void onDisconnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" disconnecting...");
	}
	
	public static void main(String[] args){
    	new BroadcastSocketIOServer().start();
    }

	public boolean addMessageQueue(Message message) {
		return messageQueue.offer(message);
	}
	
	public boolean messageQueueIsEmpty(){
		return messageQueue.isEmpty();
	}
	
	public Message takeMessageQueue(){
		try {
			return messageQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Collection<SocketIOClient> getAllClients(){
		return server.getAllClients();
	}

}
