package savemgo.nomad.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;

public class Mail {

	private static final Logger logger = LogManager.getLogger(Mail.class);

	public static void getMail(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4821, 0);
		Packets.write(ctx, 0x4823, 0);
	}

	public static void getContents(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4341);
	}

}
