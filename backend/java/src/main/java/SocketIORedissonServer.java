import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.redisson.core.MessageListener;
import org.redisson.core.RTopic;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.store.RedissonStoreFactory;
import com.corundumstudio.socketio.store.pubsub.BaseStoreFactory;
import com.corundumstudio.socketio.store.pubsub.ConnectMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.why.game.chat.Protocol;
import com.why.game.chat.proto.ChatProtoEncoder;

/**
 * 与{@link SocketIOProtoServer}类似，只是这里添加了redis（redisson封装的）作为存储客户端数据的地方，而不是默认的内存存储（虽然内存存储啥也没做）
 */
public class SocketIORedissonServer implements ConnectListener, DisconnectListener, PubSubListener<ConnectMessage>{
    
    private static final String HOST = "localhost";
    private static final int PORT = 3001;
    
    //单机模式
    private static final String SINGLE_SERVER = "localhost:6381";
    
    //集群模式只需要写一个redis cluster模式下的服务器地址，
    //因为redisson也和jedis一样会自动识别其他cluster模式下master和slave
    private static final String CLUSTER_SERVER = "localhost:7000";
    
    private static final boolean useAnnotationListener = false;
    
    private final SocketIOServer server;
    
    private RedissonClient redisson;
    private final PubSubStore pubSubStore;
    
    public SocketIORedissonServer(){
        server = new SocketIOServer(config());
        pubSubStore = server.getConfiguration().getStoreFactory().pubSubStore();
        
//        pubSubStore.subscribe("connected", new PubSubListener<ConnectMessage>() {
//            @Override
//            public void onMessage(ConnectMessage message) {
//                System.out.println("onMessage3: " + message.getNodeId() + ", " + message.getSessionId());
//            }
//        }, ConnectMessage.class);
        
        pubSubStore.subscribe("broadcast", new PubSubListener<BroadcastMessage>() {
            @Override
            public void onMessage(BroadcastMessage message) {
                System.out.println("broadcastMessage: " + message.getNodeId());
                
                for(final SocketIOClient socketClient:server.getBroadcastOperations().getClients()){
                    System.out.println("SessionId: "+socketClient.getSessionId()+", "+socketClient.get("sessionId"));
                    executor.execute(new Runnable(){
                        @Override
                        public void run() {
                            socketClient.sendEvent("message", message.getData());
                        }
                    });
                }
            }
        }, BroadcastMessage.class);
    }
    
    private Configuration config(){
        Configuration socketIOConfig = new Configuration();
        socketIOConfig.setHostname(HOST);
        socketIOConfig.setPort(PORT);
        socketIOConfig.setMaxFramePayloadLength(1024 * 1024);
        socketIOConfig.setMaxHttpContentLength(1024 * 1024);
        
        Config redissonConfig = new Config();
        redissonConfig.useClusterServers().addNodeAddress(CLUSTER_SERVER);
        //redissonConfig.useSingleServer().setAddress(SINGLE_SERVER);
        this.redisson = Redisson.create(redissonConfig);
//        RTopic<ConnectMessage> topic = redisson.getTopic("connected");
//        topic.addListener(new MessageListener<ConnectMessage>() {
//            @Override
//            public void onMessage(String channel, ConnectMessage message) {
//                System.out.println("onMessage1: "+message.getNodeId()+", "+message.getSessionId());
//            }
//        });
        
//        RMap<String, String> map = redisson.getMap("anyMap");
//        String str = map.put("123", "123");
//        System.out.println(map.get("123"));
        
        BaseStoreFactory baseStoreFactory = new RedissonStoreFactory(redisson);
        socketIOConfig.setStoreFactory(baseStoreFactory);
        return socketIOConfig;
    }
    
    public void start(){
        addListeners();
        
        server.start();
        System.out.println("\n------ "+this.getClass().getSimpleName()+"start on "+PORT+" ------\n");
    }
    
    private void addListeners(){
        if(useAnnotationListener){
            server.addListeners(new MessageAnnotation(pubSubStore));
        }else{
            server.addConnectListener(this);
            server.addDisconnectListener(this);

            server.addEventListener("message", byte[].class, new DataListener<byte[]>() {
                @Override
                public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
                    Message message = Message.parse(data);
                    System.out.println("Received: " + message.getText());
                    // Transform the text to upper case
                    message.setText(message.getText().toUpperCase());
                    // Re-encode it and send it back
                    //client.sendEvent("message", message.toByteArray());
                    //server.getBroadcastOperations().sendEvent("message", message.toByteArray());
                    broadcast(message.toByteArray());
                    System.out.println("Sent: " + message.getText());
                    
                }
            });
        }
    }
    
    private final Executor executor = Executors.newCachedThreadPool();
    
    private void broadcast(final byte[] msg){
        //为什么使用上面的无论是全局广播还是房间广播的操作都不行呢？
        //只能使用如下for一个个去发送吗？
        for(final SocketIOClient socketClient:server.getBroadcastOperations().getClients()){
            //System.out.println("SessionId: "+socketClient.getSessionId());
            executor.execute(new Runnable(){
                @Override
                public void run() {
                    socketClient.sendEvent("message", msg);
                }
            });
        }
        
        pubSubStore.publish("broadcast", new BroadcastMessage(msg));
    }
    
    public void stop(){
        server.stop();
    }

    @Override
    public void onConnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        client.set("sessionId", sessionId);
        System.out.println(sessionId+" connecting..."+client.get("sessionId"));
//        RTopic<ConnectMessage> topic = redisson.getTopic("connected");
//        topic.publish(new ConnectMessage(client.getSessionId()));
        pubSubStore.publish("connected", new ConnectMessage(client.getSessionId()));
        
        //testRedissonAdd();
    }
    
    private void testRedissonAdd(){
        List<Integer> list = redisson.getList("list");
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        
        Message message = new Message("text_"+new Random().nextInt());
        List<Message> msgList = redisson.getList("msgList");
        msgList.add(message);
    }
    
    @Override
    public void onMessage(ConnectMessage message) {
        System.out.println("onMessage2: "+message.getNodeId()+", "+message.getSessionId());
    }
    
    @Override
    public void onDisconnect(SocketIOClient client) {
        String sessionId = client.getSessionId().toString();
        System.out.println(sessionId+" disconnecting..."+client.get("sessionId"));
        client.del("sessionId");
        
        //testRedissonFind();
    }
    
    private void testRedissonFind(){
        System.out.println(Arrays.toString(redisson.getList("list").toArray()));
        System.out.println(Arrays.toString(redisson.getList("msgList").toArray()));
    }
    
    public static void main(String[] args){
        new SocketIORedissonServer().start();
    }

}
