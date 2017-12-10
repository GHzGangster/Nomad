package savemgo.nomad.chat;

public class ChatMessage {

	private MessageRecipient recipient;
	private String message;

	public ChatMessage(MessageRecipient recipient, String message) {
		this.recipient = recipient;
		this.message = message;
	}

	public MessageRecipient getRecipient() {
		return recipient;
	}

	public void setRecipient(MessageRecipient recipient) {
		this.recipient = recipient;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}