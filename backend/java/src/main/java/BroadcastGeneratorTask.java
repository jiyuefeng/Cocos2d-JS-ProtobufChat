import java.util.Random;

public class BroadcastGeneratorTask implements Runnable{
	
	private static final Random rnd = new Random();
	
	private final BroadcastSocketIOServer server;
	
	public BroadcastGeneratorTask(BroadcastSocketIOServer server){
		this.server = server;
	}

	@Override
	public void run(){
		addGenerateMessage();
	}
	
	public void addGenerateMessage(){
		server.addMessageQueue(generateMessage());
	}
	
	private Message generateMessage(){
		Message message = new Message();
		message.setText(rnd.nextInt()+"");
		return message;
	}
	
}
