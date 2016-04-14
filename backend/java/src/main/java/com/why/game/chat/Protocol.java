package com.why.game.chat;

public class Protocol {

	public enum MSG{
		message,
		changeName,
		join,
		rooms;
	}
	
	public enum RESULT{
		nameResult,
		joinResult
	}
	
}
