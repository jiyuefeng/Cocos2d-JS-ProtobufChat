import com.corundumstudio.socketio.SocketIOClient;

public class BroadcastMessageTask implements Runnable{
	
	private final BroadcastSocketIOServer server;
	
	public BroadcastMessageTask(BroadcastSocketIOServer server){
		this.server = server;
	}

	@Override
	public void run(){
		try {
			sendMsgToAllPlayers();
		} catch (Exception e) {	//catch all exceptions to ensure this schedule task continue
			e.printStackTrace();
			System.out.println("send msg to all player occur error!"+e.getMessage());
		}
	}
	
	public void sendMsgToAllPlayers(){
		if(server.messageQueueIsEmpty()){
			return;
		}
		Message message = server.takeMessageQueue();
		if(message == null){
			return;
		}
		for(SocketIOClient client:server.getAllClients()){
			if(!client.isChannelOpen()){
				continue;
			}
        	client.sendEvent("message", message.toByteArray());
        }
	}
	
}
