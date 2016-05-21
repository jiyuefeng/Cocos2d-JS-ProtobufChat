import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.RMap;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.store.RedissonStoreFactory;

/**
 * 与{@link SocketIOProtoServer}类似，只是这里添加了redis（redisson封装的）作为存储客户端数据的地方，而不是默认的内存存储（虽然内存存储啥也没做）
 */
public class SocketIORedissonServer implements ConnectListener, DisconnectListener{
	
	private static final String HOST = "localhost";
	private static final int PORT = 3001;
	
	//单机模式
	private static final String SINGLE_SERVER = "localhost:6381";
	
	//集群模式只需要写一个redis cluster模式下的服务器地址，
	//因为redisson也和jedis一样会自动识别其他cluster模式下master和slave
	private static final String CLUSTER_SERVER = "localhost:7000";
	
	private final SocketIOServer server;
	
	public SocketIORedissonServer(){
        server = new SocketIOServer(config());
	}
	
	private Configuration config(){
	    Configuration socketIOConfig = new Configuration();
	    socketIOConfig.setHostname(HOST);
	    socketIOConfig.setPort(PORT);
	    socketIOConfig.setMaxFramePayloadLength(1024 * 1024);
	    socketIOConfig.setMaxHttpContentLength(1024 * 1024);
        
        Config redissonConfig = new Config();
        redissonConfig.useClusterServers().addNodeAddress(CLUSTER_SERVER);
        //redissonConfig.useSingleServer().setAddress(SINGLE_SERVER_ADDRESS);
        RedissonClient redisson = Redisson.create(redissonConfig);
        
//        RMap<String, String> map = redisson.getMap("anyMap");
//        String str = map.put("123", "123");
//        System.out.println(map.get("123"));
        
        socketIOConfig.setStoreFactory(new RedissonStoreFactory(redisson));
        return socketIOConfig;
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
	}
	
	public void stop(){
	    server.stop();
	}

	@Override
	public void onConnect(SocketIOClient client) {
	    String sessionId = client.getSessionId().toString();
	    client.set("sessionId", sessionId);
	    System.out.println(sessionId+" connecting..."+client.get("sessionId"));
	}
	
	@Override
	public void onDisconnect(SocketIOClient client) {
	    String sessionId = client.getSessionId().toString();
		System.out.println(sessionId+" disconnecting..."+client.get("sessionId"));
		client.del(sessionId);
	}
	
	public static void main(String[] args){
    	new SocketIORedissonServer().start();
    }

}
