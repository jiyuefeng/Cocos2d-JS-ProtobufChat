package com.why.game.chat;

import com.why.game.chat.ChatProtocolBuffer.JoinResultProto;
import com.why.game.chat.ChatProtocolBuffer.MessageProto;
import com.why.game.chat.ChatProtocolBuffer.NameResultProto;

public class ChatProtoEncoder {

	public static NameResultProto nameResultProto(String name){
		NameResultProto.Builder builder = NameResultProto.newBuilder();
		builder.setSuccess(true);
		builder.setName(name);
		return builder.build();
	}
	
	public static JoinResultProto joinResultProto(String roomName){
		JoinResultProto.Builder builder = JoinResultProto.newBuilder();
		builder.setRoom(roomName);
		return builder.build();
	}
	
	public static MessageProto messageProto(String text){
		MessageProto.Builder builder = MessageProto.newBuilder();
		builder.setText(text);
		return builder.build();
	}
	
}
