package savemgo.nomad.lobby;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.helper.Gate;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.server.Lobby;

@Sharable
public class GateLobby extends Lobby {

	public GateLobby() {

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

		/** Main Lobby */
		case 0x2005:
			Gate.getLobbyList(ctx);
			break;

		case 0x2008:
			Gate.getNews(ctx);
			break;

		default:
			System.out.println("Couldn't handle command " + Integer.toHexString(in.getCommand()));
			return false;
		}

		return true;
	}

}
