package com.why.game.chat.json;

public class ChatJsonEncoder {

	public static ChatJson.NameResult nameResultProto(String name) {
		return new ChatJson.NameResult(true, name);
	}
	
	public static ChatJson.NameResult failNameResultProto(String failMsg) {
		return new ChatJson.NameResult(false, failMsg);
	}

	public static ChatJson.JoinResult joinResultProto(String roomName) {
		return new ChatJson.JoinResult(roomName);
	}

	public static ChatJson.Message messageProto(String text) {
		return new ChatJson.Message(text);
	}

}
