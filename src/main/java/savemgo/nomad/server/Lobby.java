package savemgo.nomad.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import savemgo.nomad.packet.Packet;

public abstract class Lobby extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger(Lobby.class.getSimpleName());

	private int id;

	public Lobby(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public abstract boolean readPacket(ChannelHandlerContext ctx, Packet in);

	public void onChannelActive(ChannelHandlerContext ctx) {

	}

	public void onChannelInactive(ChannelHandlerContext ctx) {

	}

	@Override
	public final void channelRead(ChannelHandlerContext ctx, Object msg) {
		Packet in = (Packet) msg;

		boolean wrote = false;
		try {
			wrote = readPacket(ctx, in);
		} catch (Exception e) {
			logger.error("Error in channel read.", e);
		} finally {
			in.release();
		}
		
		if (wrote) {
			ctx.flush();
		}
	}

	@Override
	public final void channelActive(ChannelHandlerContext ctx) {
		logger.debug("Channel opened.");
		onChannelActive(ctx);
	}

	@Override
	public final void channelInactive(ChannelHandlerContext ctx) {
		logger.debug("Channel closed.");
		onChannelInactive(ctx);
	}

}
