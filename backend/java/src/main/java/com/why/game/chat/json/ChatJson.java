package com.why.game.chat.json;

public class ChatJson {

	public static class NameResult{
		private boolean success;
		private String name;
		public NameResult() {
		}
		public NameResult(boolean success, String name) {
			this.success = success;
			this.name = name;
		}
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class JoinResult{
		private String room;
		public JoinResult() {
		}
		public JoinResult(String room) {
			this.room = room;
		}
		public String getRoom() {
			return room;
		}
		public void setRoom(String room) {
			this.room = room;
		}
	}
	
	public static class Message{
		private String text;
		private String room;
		public Message() {
		}
		public Message(String text) {
			this.text = text;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public String getRoom() {
			return room;
		}
		public void setRoom(String room) {
			this.room = room;
		}
	}
	
	public static class ChangeNameCmd{
		private String userName;
		public ChangeNameCmd() {
		}
		public ChangeNameCmd(String userName) {
			this.userName = userName;
		}
		public String getUserName() {
			return userName;
		}
		public void setUserName(String userName) {
			this.userName = userName;
		}
	}
	
	public static class JoinCmd{
		private String newRoom;
		public JoinCmd() {
		}
		public JoinCmd(String newRoom) {
			this.newRoom = newRoom;
		}
		public String getNewRoom() {
			return newRoom;
		}
		public void setNewRoom(String newRoom) {
			this.newRoom = newRoom;
		}
	}
	
}
