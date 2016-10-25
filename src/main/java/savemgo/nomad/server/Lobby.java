package savemgo.nomad.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import savemgo.nomad.packet.Packet;

public abstract class Lobby extends ChannelInboundHandlerAdapter {

	public Lobby() {

	}

	public abstract boolean readPacket(ChannelHandlerContext ctx, Packet in);

	public void onChannelActive(ChannelHandlerContext ctx) {

	}

	public void onChannelInactive(ChannelHandlerContext ctx) {

	}

	@Override
	public final void channelRead(ChannelHandlerContext ctx, Object msg) {
		Packet in = (Packet) msg;

		if (readPacket(ctx, in)) {
			ctx.flush();
		}

		in.release();
	}

	@Override
	public final void channelActive(ChannelHandlerContext ctx) {
		System.out.println("Channel opened.");
		onChannelActive(ctx);
	}

	@Override
	public final void channelInactive(ChannelHandlerContext ctx) {
		System.out.println("Channel closed.");
		onChannelInactive(ctx);
	}

}
