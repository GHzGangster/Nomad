package savemgo.nomad.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.helper.Accounts;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;

@Sharable
public class AccountLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger(AccountLobby.class);

	public AccountLobby(int id) {
		super(id, 1, 0);
	}

	@Override
	public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** Account */
		case 0x3003:
			Accounts.checkSession(ctx, in, getId(), false);
			break;

		case 0x3042:
			// Request Personal Information - MGO1
			Packets.write(ctx, 0x3041);
			break;

		case 0x3048:
			Accounts.getCharacterList(ctx);
			break;

		case 0x3101:
			Accounts.createCharacter(ctx, in);
			break;

		case 0x3103:
			Accounts.selectCharacter(ctx, in);
			break;

		case 0x3105:
			Accounts.deleteCharacter(ctx, in);
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
		Accounts.onLobbyDisconnected(ctx, getId());
	}
	
}
