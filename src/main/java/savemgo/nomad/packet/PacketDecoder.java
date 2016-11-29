package savemgo.nomad.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import savemgo.nomad.Util;

@Sharable
public class PacketDecoder extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LogManager.getLogger(PacketDecoder.class.getSimpleName());

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf buffer = null, packetBuffer = null;
		try {
			buffer = (ByteBuf) msg;

			int readable = buffer.readableBytes();

			if (readable == 0) {
				logger.debug("No bytes to read in.");
				return;
			}

			if (readable < Packet.OFFSET_PAYLOAD) {
				logger.debug("Packet is too short, waiting for header...");
				return;
			}

			short lengthPayload = buffer.getShort(buffer.readerIndex() + 2);
			lengthPayload ^= Util.KEY_XOR & 0xffff;

			if (lengthPayload < 0 || Packet.MAX_PAYLOAD_LENGTH < lengthPayload) {
				logger.debug("Payload isn't a valid length.");
				return;
			}

			int bytesToRead = Packet.OFFSET_PAYLOAD + lengthPayload;

			if (readable < bytesToRead) {
				logger.debug("Packet is too short, waiting for payload...");
				return;
			}

			packetBuffer = ctx.alloc().directBuffer(bytesToRead);
			packetBuffer.writeBytes(buffer, bytesToRead);

			Util.xor(packetBuffer, packetBuffer.readableBytes(), Util.KEY_XOR);

			ByteBuf header = packetBuffer.copy(Packet.OFFSET_COMMAND, Packet.OFFSET_PAYLOAD);
			ByteBuf payload = packetBuffer.copy(Packet.OFFSET_PAYLOAD, lengthPayload);

			Packet packet = new Packet(header, payload);

			if (!packet.validate()) {
				logger.warn("Packet is invalid.");
				return;
			}

			logger.debug("In - Command " + Integer.toHexString(packet.getCommand()) + " - " + packet.getPayloadLength()
					+ " bytes");
			if (packet.getPayloadLength() > 0) {
				logger.debug("# {}", () -> ByteBufUtil.hexDump(packet.getPayload()));
			}

			final ByteBuf packetBufferFinal = packetBuffer;
			logger.debug(() -> ByteBufUtil.hexDump(packetBufferFinal));

			ctx.fireChannelRead(packet);
		} catch (Exception e) {
			logger.error("Failed to decode packet.", e);
		} finally {
			if (buffer != null) {
				buffer.release();
			}
			if (packetBuffer != null) {
				packetBuffer.release();
			}
		}
	}

}
