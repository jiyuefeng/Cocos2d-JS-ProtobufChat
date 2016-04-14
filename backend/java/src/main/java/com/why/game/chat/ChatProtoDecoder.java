package com.why.game.chat;

import com.google.protobuf.InvalidProtocolBufferException;
import com.why.game.chat.ChatProtocolBuffer.MessageProto;

public class ChatProtoDecoder {

	public static MessageProto message(byte[] data){
		try {
			return MessageProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}
	
}
