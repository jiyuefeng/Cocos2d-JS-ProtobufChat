package com.why.game.chat.json;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
import com.why.game.chat.Protocol;
import com.why.game.chat.json.ChatJson.ChangeNameCmd;
import com.why.game.chat.json.ChatJson.JoinCmd;
import com.why.game.chat.json.ChatJson.Message;

public class ChatJsonServer implements ConnectListener, DisconnectListener{

	private static final String USER_NAME_PREFIX = "Guest";
	private static final String DEFAULT_ROOM = "Lobby";
	
	private final AtomicInteger guestNum = new AtomicInteger(0);
	private final Map<UUID, String> userNameMap = new HashMap<UUID, String>();
	private final Set<String> usedNames = new HashSet<String>();
	private final Map<UUID, String> userRoomMap = new HashMap<UUID, String>();
	
	private final Executor executor = Executors.newCachedThreadPool();
	private final SocketIOServer server;
	
	public ChatJsonServer(){
        server = new SocketIOServer(config());
	}
	
	private Configuration config(){
	    Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3001);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        return config;
	}
	
	public void start(){
        server.addConnectListener(this);
        server.addDisconnectListener(this);
        
        handleBroadcastMessage();
        handleChangeUserName();
        handleJoinOtherRoom();
        handleQueryRooms();
        
        server.start();

//        try {
//			Thread.sleep(Integer.MAX_VALUE);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//        server.stop();
	}
	
	public void stop(){
	    server.stop();
	}
	
	@Override
	public void onConnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" connecting...");
		assignGuestName(client);
        joinRoom(client, DEFAULT_ROOM);
	}
	
	private int assignGuestName(SocketIOClient client) {
		String userName = USER_NAME_PREFIX+guestNum;
		userNameMap.put(client.getSessionId(), userName);
		client.sendEvent(Protocol.RESULT.nameResult.name(), ChatJsonEncoder.nameResultProto(userName));
		usedNames.add(userName);
		return guestNum.incrementAndGet();
	}

	private void joinRoom(SocketIOClient client, String roomName) {
		Namespace room = (Namespace)server.addNamespace(roomName);
		room.onConnect(client);
		room.addClient(client);
		//namespace.join(room, client.getSessionId());
		//client.joinRoom(roomName);
		UUID sessionId = client.getSessionId();
		userRoomMap.put(sessionId, roomName);
		
		client.sendEvent(Protocol.RESULT.joinResult.name(), 
				ChatJsonEncoder.joinResultProto(roomName));
		client.sendEvent(Protocol.MSG.message.name(), 
				ChatJsonEncoder.messageProto(usersInRoomSummary(roomName)));
		
//		server.getRoomOperations(roomName).sendEvent(Protocol.MSG.message.name(), 
//				ChatProtoEncoder.messageProto(userNameMap.get(sessionId)+" has joined "+roomName+'!').toByteArray());
		systemBroadcast(client, room, userNameMap.get(sessionId)+" has joined "+roomName+'!');
	}

	private String usersInRoomSummary(String roomName) {
		StringBuilder sb = new StringBuilder("Users currently in "+roomName+": ");
		//System.out.println("roomName="+roomName);
		SocketIONamespace room = server.getNamespace(roomName);
		if(room == null){
			//System.out.println(roomName+"=null");
			return "";
		}
		//System.out.println("roomName client size:"+room.getAllClients().size());
		for(SocketIOClient client:room.getAllClients()){
			sb.append(userNameMap.get(client.getSessionId())).append(",");
		}
		sb.deleteCharAt(sb.length()-1).append("!");
		return sb.toString();
	}
	
	private void handleBroadcastMessage(){
		server.addEventListener(Protocol.MSG.message.name(), Message.class, new DataListener<Message>() {
            @Override
            public void onData(SocketIOClient client, Message message, AckRequest ackRequest) {
            	String roomName = message.getRoom();
            	//System.out.println("onData roomName="+roomName);
            	SocketIONamespace room = server.getNamespace(roomName);
            	BroadcastOperations broadcastOperations = server.getRoomOperations(roomName);
            	//System.out.println("broadcastOperations size="+broadcastOperations.getClients().size());
            	//System.out.println("room size="+room.getAllClients().size());
            	//System.out.println("room size="+room.getAllClients().size());
            	
            	clientBroadcast(client, room, message.getText());
            }
        });
	}
	
	private void handleChangeUserName(){
		server.addEventListener(Protocol.MSG.changeName.name(), ChangeNameCmd.class, new DataListener<ChangeNameCmd>() {
			@Override
			public void onData(SocketIOClient client, ChangeNameCmd data, AckRequest ackSender) {
				String userName = data.getUserName();
				if(userName.indexOf(USER_NAME_PREFIX) == 0){
					client.sendEvent(Protocol.RESULT.nameResult.name(), 
							ChatJsonEncoder.failNameResultProto("Names cannot begin with "+USER_NAME_PREFIX));
				}else{
					if(usedNames.contains(userName)){
						client.sendEvent(Protocol.RESULT.nameResult.name(), 
								ChatJsonEncoder.failNameResultProto("That name is already in use!"));
					}else{
						UUID sessionId = client.getSessionId();
						String preUserName = userNameMap.get(sessionId);
						usedNames.add(userName);
						userNameMap.put(sessionId, userName);
						usedNames.remove(preUserName);
						
						client.sendEvent(Protocol.RESULT.nameResult.name(), 
								ChatJsonEncoder.nameResultProto(userName));
						
						SocketIONamespace room = server.getNamespace(userRoomMap.get(sessionId));
						systemBroadcast(client, room, preUserName+" is now known as ["+userName+"]!");
					}
				}
			}
		});
	}
	
	private void handleJoinOtherRoom(){
		server.addEventListener(Protocol.MSG.join.name(), JoinCmd.class, new DataListener<JoinCmd>() {
			@Override
			public void onData(SocketIOClient client, JoinCmd data, AckRequest ackSender) {
				String newRoom = data.getNewRoom();
				Namespace room = leaveRoom(client);
				systemBroadcast(client, room, userNameMap.get(client.getSessionId())+" changed room to ["+newRoom+"]!");
				joinRoom(client, newRoom);
			}
		});
	}
	
	private void clientBroadcast(SocketIOClient client, SocketIONamespace room, String msg){
		broadcast(false, client, room, msg);
	}
	
	private void systemBroadcast(SocketIOClient client, SocketIONamespace room, String msg){
		broadcast(true, client, room, msg);
	}
	
	private void broadcast(final boolean isSystem, final SocketIOClient client, final SocketIONamespace room, final String msg){
		//broadcastOperations.sendEvent(Protocol.MSG.message.name(), 
		//		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
		
		
		//room.getBroadcastOperations().sendEvent(Protocol.MSG.message.name(), 
		//		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
		
		//为什么使用上面的无论是全局广播还是房间广播的操作都不行呢？
		//只能使用如下for一个个去发送吗？
		for(final SocketIOClient socketClient:room.getAllClients()){
			//System.out.println("SessionId: "+socketClient.getSessionId());
			if(socketClient.getSessionId() == client.getSessionId()){
				continue;
			}
			executor.execute(new Runnable(){
				@Override
				public void run() {
					socketClient.sendEvent(Protocol.MSG.message.name(), 
				    		ChatJsonEncoder.messageProto((isSystem ? "" : userNameMap.get(client.getSessionId())+": ")+msg));
				}
			});
		}
	}
	
	private void handleQueryRooms(){
		server.addEventListener(Protocol.MSG.rooms.name(), String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data, AckRequest ackSender) {
				Collection<SocketIONamespace> existsRooms = server.getAllNamespaces();
				List<String> roomNames = new ArrayList<String>(existsRooms.size());
				for(SocketIONamespace room:existsRooms){
					roomNames.add(room.getName());
				}
				client.sendEvent(Protocol.MSG.rooms.name(), roomNames);
			}
		});
	}
	
	@Override
	public void onDisconnect(SocketIOClient client) {
		UUID sessionId = client.getSessionId();
		System.out.println(sessionId+" disconnecting...");
		leaveRoom(client);
		userRoomMap.remove(sessionId);
		usedNames.remove(userNameMap.remove(sessionId));
	}
	
	private Namespace leaveRoom(SocketIOClient client){
		UUID sessionId = client.getSessionId();
		String roomName = userRoomMap.get(sessionId);
		Namespace room = (Namespace)server.getNamespace(roomName);
		room.onDisconnect(client);
		System.out.println("leave "+roomName+" left client:"+room.getAllClients().size());
		//room.leaveRoom(roomName, client.getSessionId());
		//client.leaveRoom();
		if(room.getAllClients().isEmpty()){
			server.removeNamespace(roomName);
		}
		
		systemBroadcast(client, room, usersInRoomSummary(roomName));
		return room;
	}
	
    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
    	new ChatJsonServer().start();
    }

}
