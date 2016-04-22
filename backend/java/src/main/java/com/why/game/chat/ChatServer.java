package com.why.game.chat;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class ChatServer implements ConnectListener, DisconnectListener{

	private static final String USER_NAME_PREFIX = "Guest";
	private static final String DEFAULT_ROOM = "Lobby";
	
	private final AtomicInteger guestNum = new AtomicInteger(0);
	private final Map<UUID, String> userNames = new HashMap<UUID, String>();
	private final Set<String> namesUsed = new HashSet<String>();
	
	private final Map<UUID, String> currentRoom = new HashMap<UUID, String>();
	
	private final SocketIOServer server;
	
	public ChatServer(){
		Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3001);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        server = new SocketIOServer(config);
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
		StringBuilder sb = new StringBuilder("Users currently in "+roomName+": ");
		//System.out.println("roomName="+roomName);
		SocketIONamespace room = server.getNamespace(roomName);
		if(room == null){
			//System.out.println(roomName+"=null");
			return "";
		}
		//System.out.println("roomName client size:"+room.getAllClients().size());
		for(SocketIOClient client:room.getAllClients()){
			sb.append(userNames.get(client.getSessionId())).append(",");
		}
		sb.deleteCharAt(sb.length()-1).append("!");
		return sb.toString();
	}
	
	private void handleBroadcastMessage(){
		server.addEventListener(Protocol.MSG.message.name(), byte[].class, new DataListener<byte[]>() {
            @Override
            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
            	MessageProto message = ChatProtoDecoder.message(data);
            	
            	String roomName = message.getRoom();
            	//System.out.println("onData roomName="+roomName);
            	SocketIONamespace room = server.getNamespace(roomName);
            	BroadcastOperations broadcastOperations = server.getRoomOperations(roomName);
            	//System.out.println("broadcastOperations size="+broadcastOperations.getClients().size());
            	//System.out.println("room size="+room.getAllClients().size());
            	//System.out.println("room size="+room.getAllClients().size());
            	
            	broadcast(client, room, message.getText());
            }
        });
	}
	
	private void broadcast(SocketIOClient client, SocketIONamespace room, String msg){
		//    	broadcastOperations.sendEvent(Protocol.MSG.message.name(), 
		//		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
		
		
		//room.getBroadcastOperations().sendEvent(Protocol.MSG.message.name(), 
		//		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
		
		//为什么使用上面的无论是全局广播还是房间广播的操作都不行呢？
		//只能使用如下for一个个去发送吗？
		for(SocketIOClient socketClient:room.getAllClients()){
			//System.out.println("SessionId: "+socketClient.getSessionId());
			if(socketClient.getSessionId() == client.getSessionId()){
				continue;
			}
			socketClient.sendEvent(Protocol.MSG.message.name(), 
		    		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+msg).toByteArray());
		}
	}
	
	private void handleChangeUserName(){
		server.addEventListener(Protocol.MSG.changeName.name(), byte[].class, new DataListener<byte[]>() {
			@Override
			public void onData(SocketIOClient client, byte[] data, AckRequest ackSender) {
				String userName = ChatProtoDecoder.changeNameCmd(data).getUserName();
				if(userName.indexOf(USER_NAME_PREFIX) == 0){
					client.sendEvent(Protocol.RESULT.nameResult.name(), 
							ChatProtoEncoder.failNameResultProto("Names cannot begin with "+USER_NAME_PREFIX).toByteArray());
				}else{
					if(namesUsed.contains(userName)){
						client.sendEvent(Protocol.RESULT.nameResult.name(), 
								ChatProtoEncoder.failNameResultProto("That name is already in use!").toByteArray());
					}else{
						UUID sessionId = client.getSessionId();
						String preUserName = userNames.get(sessionId);
						namesUsed.add(userName);
						userNames.put(sessionId, userName);
						namesUsed.remove(preUserName);
						
						client.sendEvent(Protocol.RESULT.nameResult.name(), 
								ChatProtoEncoder.nameResultProto(userName).toByteArray());
						
						SocketIONamespace room = server.getNamespace(currentRoom.get(sessionId));
						broadcast(client, room, preUserName+" is now known as ["+userName+"]!");
					}
				}
			}
		});
	}
	
	private void handleJoinOtherRoom(){
		server.addEventListener(Protocol.MSG.join.name(), byte[].class, new DataListener<byte[]>() {
			@Override
			public void onData(SocketIOClient client, byte[] data, AckRequest ackSender) {
				String newRoom = ChatProtoDecoder.joinCmd(data).getNewRoom();
				UUID sessionId = client.getSessionId();
				String roomName = currentRoom.get(sessionId);
				Namespace room = (Namespace)server.getNamespace(roomName);
				//room.leave(roomName, sessionId);
				//room.leaveRoom(roomName, sessionId);
				room.onDisconnect(client);
				
				broadcast(client, room, userNames.get(sessionId)+" changed room to ["+newRoom+"]!");
				broadcast(client, room, usersInRoomSummary(roomName));
				
				joinRoom(client, newRoom);
			}
		});
	}
	
	private void handleQueryRooms(){
		server.addEventListener(Protocol.MSG.rooms.name(), byte[].class, new DataListener<byte[]>() {
			@Override
			public void onData(SocketIOClient client, byte[] data, AckRequest ackSender) {
				Collection<SocketIONamespace> existsRooms = server.getAllNamespaces();
				List<String> roomNames = new ArrayList<String>(existsRooms.size());
				for(SocketIONamespace room:existsRooms){
					roomNames.add(room.getName());
				}
				client.sendEvent(Protocol.MSG.rooms.name(), 
						ChatProtoEncoder.roomsProto(roomNames).toByteArray());
			}
		});
	}
	
	@Override
	public void onDisconnect(SocketIOClient client) {
		System.out.println(client.getSessionId()+" disconnecting...");
		String roomName = currentRoom.get(client.getSessionId());
		Namespace room = (Namespace)server.getNamespace(roomName);
		room.onDisconnect(client);
		//room.leaveRoom(roomName, client.getSessionId());
		//client.leaveRoom();
	}
	
    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
    	new ChatServer().start();
    }

}
