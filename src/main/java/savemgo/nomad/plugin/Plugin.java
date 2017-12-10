package savemgo.nomad.plugin;

import org.hibernate.cfg.Configuration;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.chat.ChatMessage;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.User;
import savemgo.nomad.packet.Packet;

public class Plugin {

	public void initialize() {

	}

	public void onStart() {

	}

	public void addAnnotatedClass(Configuration configuration) {

	}

	public int handleGameLobbyCommand(ChannelHandlerContext ctx, Packet in) {
		return -1;
	}

	public ChatMessage handleChatCommand(User user, String message) {
		return null;
	}

	public void gameNCheck(Game game) {

	}

}
