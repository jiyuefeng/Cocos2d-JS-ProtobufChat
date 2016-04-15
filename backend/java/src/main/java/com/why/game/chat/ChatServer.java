package com.why.game.chat;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.namespace.Namespace;
import com.why.game.chat.ChatProtocolBuffer.MessageProto;

public class ChatServer {

	private static final String USER_NAME_PREFIX = "Guest";
	private static final String DEFAULT_ROOM = "Lobby";
	
	private final AtomicInteger guestNum = new AtomicInteger(0);
	private final Map<UUID, String> userNames = new HashMap<UUID, String>();
	private final Set<String> namesUsed = new HashSet<String>();
	
	private final Map<UUID, String> currentRoom = new HashMap<UUID, String>();
	
	public void start(){
		Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3001);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);

        final SocketIOServer server = new SocketIOServer(config);
        server.addConnectListener(new ConnectListener(){
        	@Override
        	public void onConnect(SocketIOClient client) {
        		System.out.println(client.getSessionId()+" connecting...");
        		assignGuestName(client);
                joinRoom(client, DEFAULT_ROOM);
        	}
        	
			private int assignGuestName(SocketIOClient client) {
        		String userName = USER_NAME_PREFIX+guestNum;
        		userNames.put(client.getSessionId(), userName);
        		client.sendEvent(Protocol.RESULT.nameResult.name(), ChatProtoEncoder.nameResultProto(userName).toByteArray());
        		namesUsed.add(userName);
        		return guestNum.incrementAndGet();
        	}

			private void joinRoom(SocketIOClient client, String room) {
				Namespace namespace = (Namespace)server.addNamespace(room);
				namespace.onConnect(client);
				namespace.addClient(client);
				//namespace.join(room, client.getSessionId());
				//client.joinRoom(roomName);
				currentRoom.put(client.getSessionId(), room);
				
				client.sendEvent(Protocol.RESULT.joinResult.name(), 
						ChatProtoEncoder.joinResultProto(room).toByteArray());
				server.getRoomOperations(room).sendEvent(Protocol.MSG.message.name(), 
						ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+" has joined "+room+'!').toByteArray());
				client.sendEvent(Protocol.MSG.message.name(), 
						ChatProtoEncoder.messageProto(usersInRoomSummary(room)).toByteArray());
			}

			private String usersInRoomSummary(String roomName) {
				StringBuilder sb = new StringBuilder();
				System.out.println("roomName="+roomName);
				SocketIONamespace room = server.getNamespace(roomName);
				if(room == null){
					System.out.println(roomName+"=null");
					return "";
				}
				System.out.println("roomName client size:"+room.getAllClients().size());
				for(SocketIOClient client:room.getAllClients()){
					sb.append(userNames.get(client.getSessionId())).append(",");
				}
				sb.deleteCharAt(sb.length()-1).append("!");
				return sb.toString();
			}
			
        });
        
//        server.addEventListener("msg", byte[].class, new DataListener<byte[]>() {
//            @Override
//            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
//            	System.out.println("onData");
//                client.sendEvent("msg", data);
//            }
//        });
        
        server.addEventListener(Protocol.MSG.message.name(), byte[].class, new DataListener<byte[]>() {
            @Override
            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
            	MessageProto message = ChatProtoDecoder.message(data);
            	
            	String roomName = message.getRoom();
            	System.out.println("onData roomName="+roomName);
            	SocketIONamespace room = server.getNamespace(roomName);
            	BroadcastOperations broadcastOperations = server.getRoomOperations(roomName);
            	System.out.println(broadcastOperations.getClients().size());
            	
//            	broadcastOperations.sendEvent(Protocol.MSG.message.name(), 
//                		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
            	
            	
//            	room.getBroadcastOperations().sendEvent(Protocol.MSG.message.name(), 
//                		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
            	
            	server.getRoomOperations(roomName).sendEvent(Protocol.MSG.message.name(), 
						ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+" has joined "+roomName+'!').toByteArray());
            	
            	//为什么使用上面的无论是全局广播还是房间广播的操作都不行呢？
            	//只能使用如下for一个个去发送吗？
//            	for(SocketIOClient socketClient:broadcastOperations.getClients()){
//            		System.out.println("SessionId: "+socketClient.getSessionId());
//            		if(socketClient.getSessionId() == client.getSessionId()){
//            			continue;
//            		}
//            		socketClient.sendEvent(Protocol.MSG.message.name(), 
//                    		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
//            	}
            }
        });
        
        server.addDisconnectListener(new DisconnectListener(){
			@Override
			public void onDisconnect(SocketIOClient client) {
				System.out.println(client.getSessionId()+" disconnecting...");
				String roomName = currentRoom.get(client.getSessionId());
				Namespace room = (Namespace)server.getNamespace(roomName);
				room.onDisconnect(client);
				//room.leaveRoom(roomName, client.getSessionId());
				//client.leaveRoom();
			}
		});

        server.start();

//        try {
//			Thread.sleep(Integer.MAX_VALUE);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//        server.stop();
	}
	
    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
    	new ChatServer().start();
    }

}
