package savemgo.nomad.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.helper.Users;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;

@Sharable
public class AccountLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger(AccountLobby.class);

	public AccountLobby(Lobby lobby) {
		super(lobby);
	}

	@Override
	public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Account */
		case 0x3003:
			Users.checkSession(ctx, in, getLobby(), false);
			break;

		case 0x3042:
			// Request Personal Information - MGO1
			Packets.write(ctx, 0x3041);
			break;

		case 0x3048:
			Users.getCharacterList(ctx);
			break;

		case 0x3101:
			Users.createCharacter(ctx, in);
			break;

		case 0x3103:
			Users.selectCharacter(ctx, in);
			break;

		case 0x3105:
			Users.deleteCharacter(ctx, in);
			break;

		// case 0x3107:
		// Accounts.checkCharacterName(ctx, in);
		// break;

		default:
			logger.error("Couldn't handle command " + Integer.toHexString(in.getCommand()));
			return false;
		}

		return true;
	}

	@Override
	public void onChannelInactive(ChannelHandlerContext ctx) {
		Users.onLobbyDisconnected(ctx, getLobby());
	}
	
}
