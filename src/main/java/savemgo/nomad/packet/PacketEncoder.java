package savemgo.nomad.packet;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import savemgo.nomad.crypto.Crypto;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

@Sharable
public class PacketEncoder extends ChannelOutboundHandlerAdapter {

	private final int[] CRYPTED_IDS = { 0x4305 };

	private static final Logger logger = LogManager.getLogger(PacketEncoder.class);

	private static final AttributeKey<Integer> SEQUENCE_OUT = AttributeKey.valueOf("sequenceOut");

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		Packet packet = null;
		try {
			packet = (Packet) msg;

			final int command = packet.getCommand();
			final int lenP = (packet.getPayload() != null) ? packet.getPayload().capacity() : 0;
			final Packet fPacket = packet;

			if (lenP > 0) {
				logger.debug("{} - Out - Command {} - {} bytes", Util.getIp(ctx), String.format("%04x", command), lenP);
				logger.debug(() -> ByteBufUtil.hexDump(fPacket.getPayload()));
			} else {
				logger.debug("{} - Out - Command {}", Util.getIp(ctx), String.format("%04x", command));
			}

			int pad = 0;
			if (Packets.usesCrypto(CRYPTED_IDS, packet)) {
				int lenPayload = packet.getPayload().readableBytes();
				pad = 8 - (lenPayload % 8);
				if (pad != 0) {
					packet.getPayload().capacity(lenPayload + pad);
					packet.getPayload().writeZero(pad);
				}
				Crypto.instancePacket().encrypt(packet.getPayload());
			}

			Attribute<Integer> sequenceAttr = ctx.channel().attr(SEQUENCE_OUT);
			Integer sequenceInt = sequenceAttr.get();
			int sequence = 0;
			if (sequenceInt != null) {
				sequence = sequenceInt.intValue();
			}
			sequenceAttr.set(++sequence);

			packet.setSequence(sequence);
			packet.prepare();

			final int lengthPayload = packet.getPayloadLength();

			ByteBuf buffer = ctx.alloc().directBuffer(24 + lengthPayload);
			buffer.writeBytes(packet.getHeader(), 0, 24);
			if (packet.getPayload() != null) {
				buffer.writeBytes(packet.getPayload(), 0, lengthPayload);
			}

			Util.xor(buffer, buffer.readableBytes(), Util.KEY_XOR);
			
			// if (ctx.channel().isOpen() && ctx.channel().isWritable()) {
			ctx.write(buffer, ctx.voidPromise());
			// }
		} catch (Exception e) {
			logger.error("Failed to encode packet.", e);
		} finally {
			if (packet != null) {
				packet.release();
			}
		}
	}

}
