package savemgo.nomad.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.packet.Packet;

@Sharable
public class GateLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger(GateLobby.class);

	public GateLobby(Lobby lobby) {
		super(lobby);
	}

	@Override
	public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Main Lobby */
		case 0x2005:
			Hub.getLobbyList(ctx);
			break;

		case 0x2008:
			Hub.getNews(ctx);
			break;

		default:
			logger.error("Couldn't handle command " + Integer.toHexString(in.getCommand()));
			return false;
		}

		return true;
	}

}
