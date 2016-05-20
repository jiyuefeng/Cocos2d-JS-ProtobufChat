import com.google.protobuf.InvalidProtocolBufferException;

public class Message {

	private String text;
	
	public Message() {
		
	}
	
	public Message(String text) {
        this.text = text;
    }

	public Message(Example.Message proto){
		text = proto.getText();
	}
	
	public byte[] toByteArray(){
		Example.Message.Builder builder = Example.Message.newBuilder();
		builder.setText(text);
		return builder.build().toByteArray();
	}
	
	public static Message parse(byte[] bytes){
		Example.Message proto = null;
		try {
			proto = Example.Message.parseFrom(bytes);
		} catch (InvalidProtocolBufferException ex) {
			throw new IllegalArgumentException(ex);
		}
		return new Message(proto);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
}
