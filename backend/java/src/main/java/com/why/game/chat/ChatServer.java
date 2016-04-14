package com.why.game.chat;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
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

                handleBroadcastMessage(client);
                handleDisconnect(client);
        	}
        	
			private int assignGuestName(SocketIOClient client) {
        		String userName = USER_NAME_PREFIX+guestNum;
        		userNames.put(client.getSessionId(), userName);
        		client.sendEvent(Protocol.RESULT.nameResult.name(), ChatProtoEncoder.nameResultProto(userName).toByteArray());
        		namesUsed.add(userName);
        		return guestNum.incrementAndGet();
        	}

			private void joinRoom(SocketIOClient client, String roomName) {
				server.addNamespace(roomName);
				client.joinRoom(roomName);
				currentRoom.put(client.getSessionId(), roomName);
				client.sendEvent(Protocol.RESULT.joinResult.name(), 
						ChatProtoEncoder.joinResultProto(roomName).toByteArray());
				server.getRoomOperations(roomName).sendEvent(Protocol.MSG.message.name(), 
						ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+" has joined "+roomName+'!').toByteArray());
				client.sendEvent(Protocol.MSG.message.name(), 
						ChatProtoEncoder.messageProto(usersInRoomSummary(roomName)).toByteArray());
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
				//sb.deleteCharAt(sb.length()-1).append("!");
				return sb.toString();
			}
			
			private void handleBroadcastMessage(SocketIOClient client) {
				server.addEventListener(Protocol.MSG.message.name(), byte[].class, new DataListener<byte[]>() {
		            @Override
		            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
		            	MessageProto message = ChatProtoDecoder.message(data);
		                server.getRoomOperations(message.getRoom()).sendEvent(Protocol.MSG.message.name(), 
		                		ChatProtoEncoder.messageProto(userNames.get(client.getSessionId())+": "+message.getText()).toByteArray());
		            }
		        });
			}
			
			private void  handleDisconnect(SocketIOClient client){
				server.addDisconnectListener(new DisconnectListener(){
					@Override
					public void onDisconnect(SocketIOClient client) {
						System.out.println(client.getSessionId()+" connecting...");
						client.leaveRoom(currentRoom.get(client.getSessionId()));
					}
				});
			}

        });
        
//        server.addEventListener("msg", byte[].class, new DataListener<byte[]>() {
//            @Override
//            public void onData(SocketIOClient client, byte[] data, AckRequest ackRequest) {
//            	System.out.println("onData");
//                client.sendEvent("msg", data);
//            }
//        });

        server.start();

        try {
			Thread.sleep(Integer.MAX_VALUE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        server.stop();
	}
	
    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
    	new ChatServer().start();
    }

}
