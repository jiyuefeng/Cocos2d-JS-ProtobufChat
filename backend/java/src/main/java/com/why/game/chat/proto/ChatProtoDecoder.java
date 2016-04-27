package com.why.game.chat.proto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.why.game.chat.proto.ChatProtocolBuffer.ChangeNameCmdProto;
import com.why.game.chat.proto.ChatProtocolBuffer.JoinCmdProto;
import com.why.game.chat.proto.ChatProtocolBuffer.MessageProto;

public class ChatProtoDecoder {

	public static MessageProto message(byte[] data){
		try {
			return MessageProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ChangeNameCmdProto changeNameCmd(byte[] data){
		try {
			return ChangeNameCmdProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static JoinCmdProto joinCmd(byte[] data){
		try {
			return JoinCmdProto.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new RuntimeException(e);
		}
	}
	
}
