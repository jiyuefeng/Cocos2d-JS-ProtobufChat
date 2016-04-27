package com.why.game.chat;

import com.why.game.chat.json.ChatJsonServer;
import com.why.game.chat.proto.ChatProtoServer;

public class Server {

	public static void main(String[] args) {
		if(args.length > 0){
			String protocol = args[0];
			System.out.println();
			if(protocol.equals("protobuf")){
				new ChatProtoServer().start();
			}else if(protocol.equals("json")){
				new ChatJsonServer().start();
			}else{
				throw new IllegalArgumentException("不支持的协议参数:"+protocol);
			}
		}else{
			new ChatProtoServer().start();
		}
	}
	
}
