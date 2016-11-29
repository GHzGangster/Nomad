package savemgo.nomad.packet;

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
import savemgo.nomad.Util;

@Sharable
public class PacketEncoder extends ChannelOutboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger(PacketEncoder.class.getSimpleName());

	private static final AttributeKey<Integer> SEQUENCE_OUT = AttributeKey.valueOf("sequenceOut");

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		try {
			Packet packet = (Packet) msg;

			Attribute<Integer> sequenceAttr = ctx.channel().attr(SEQUENCE_OUT);
			Integer sequenceInt = sequenceAttr.get();
			int sequence = 0;
			if (sequenceInt != null) {
				sequence = sequenceInt.intValue();
			}
			sequenceAttr.set(++sequence);

			packet.setSequence(sequence);
			packet.prepare();

			logger.debug("Out - Command " + Integer.toHexString(packet.getCommand()) + " - " + packet.getPayloadLength()
					+ " bytes");
			if (packet.getPayloadLength() > 0) {
				logger.debug("# {}", () -> ByteBufUtil.hexDump(packet.getPayload()));
			}

			int lengthPayload = packet.getPayloadLength();

			ByteBuf buffer = ctx.alloc().directBuffer(24 + lengthPayload);
			buffer.writeBytes(packet.getHeader(), 0, 24);
			if (packet.getPayload() != null) {
				buffer.writeBytes(packet.getPayload(), 0, lengthPayload);
			}

			packet.release();

			logger.debug(() -> ByteBufUtil.hexDump(buffer));

			Util.xor(buffer, buffer.readableBytes(), Util.KEY_XOR);

			ctx.write(buffer, ctx.voidPromise());
		} catch (Exception e) {
			logger.error("Failed to encode packet.", e);
		}
	}

}
