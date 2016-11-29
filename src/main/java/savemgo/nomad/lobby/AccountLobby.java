package savemgo.nomad.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.helper.Accounts;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.Lobby;

@Sharable
public class AccountLobby extends Lobby {

	private static final Logger logger = LogManager.getLogger(AccountLobby.class.getSimpleName());

	public AccountLobby(int id) {
		super(id);
	}

	@Override
	public boolean readPacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		switch (command) {

		/** General */
		case 0x0003:
			ctx.close();
			break;

		case 0x0005:
			ctx.write(new Packet(0x0005));
			break;

		/** Account */
		case 0x3003:
			Accounts.checkSession(ctx, in);
			break;

		case 0x3042:
			logger.debug("Got 0x3042.");
			ctx.write(new Packet(0x3041));
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

}
