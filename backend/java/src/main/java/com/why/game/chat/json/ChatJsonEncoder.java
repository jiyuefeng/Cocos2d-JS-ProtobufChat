package com.why.game.chat.json;

import com.why.game.chat.json.ChatJson.JoinResult;
import com.why.game.chat.json.ChatJson.Message;
import com.why.game.chat.json.ChatJson.NameResult;

public class ChatJsonEncoder {

	public static NameResult nameResult(String name) {
		return new NameResult(true, name);
	}
	
	public static NameResult failNameResult(String failMsg) {
		return new NameResult(false, failMsg);
	}

	public static JoinResult joinResult(String roomName) {
		return new JoinResult(roomName);
	}

	public static Message message(String text) {
		return new Message(text);
	}

}
