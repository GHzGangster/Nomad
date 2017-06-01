package savemgo.nomad.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import savemgo.nomad.crypto.Crypto;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

@Sharable
public class PacketDecoder extends ChannelInboundHandlerAdapter {

	private final int[] CRYPTED_IDS = { 0x3003, 0x4310, 0x4320, 0x43c0, 0x4700, 0x4990 };

	private static final Logger logger = LogManager.getLogger(PacketDecoder.class);

	private static final AttributeKey<ByteBuf> BUFFER_IN = AttributeKey.valueOf("bufferIn");
	private static final AttributeKey<Integer> SEQUENCE_IN = AttributeKey.valueOf("sequenceIn");
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) {
		ByteBuf buffer = ctx.alloc().buffer(0x416);
		Attribute<ByteBuf> bytebufAttr = ctx.channel().attr(BUFFER_IN);
		bytebufAttr.set(buffer);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		Attribute<ByteBuf> bytebufAttr = ctx.channel().attr(BUFFER_IN);
		ByteBuf buffer = bytebufAttr.get();
		Util.safeRelease(buffer);
		bytebufAttr.set(null);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf packetBuffer = null;
		try {
			Attribute<ByteBuf> bytebufAttr = ctx.channel().attr(BUFFER_IN);
			ByteBuf buffer = bytebufAttr.get();
			
			if (msg == null || buffer == null) {
				return;
			}
			
			buffer.writeBytes((ByteBuf) msg);
			
			int readable = buffer.readableBytes();
			if (readable == 0) {
				logger.debug("No bytes to read in.");
				return;
			} else if (readable < Packet.OFFSET_PAYLOAD) {
				logger.debug("Packet is too short, waiting for header...");
				return;
			}

			int readerIndex = buffer.readerIndex();
			final int command = (buffer.getShort(readerIndex) ^ (Util.KEY_XOR >> 16)) & 0xffff;
			int lenPayload = (buffer.getShort(readerIndex + 2) ^ Util.KEY_XOR) & 0xffff;
			int pad = 0;
			if (Packets.usesCrypto(CRYPTED_IDS, command) && lenPayload % 8 != 0) {
				pad = 8 - (lenPayload % 8);
			}

			final int lengthPayload = lenPayload + pad;
			if (lengthPayload < 0 || Packet.MAX_PAYLOAD_LENGTH < lengthPayload) {
				logger.debug("Payload isn't a valid length: {}", lengthPayload);
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
				logger.warn("Packet is invalid: {}", command);
				return;
			}
			
			Attribute<Integer> sequenceAttr = ctx.channel().attr(SEQUENCE_IN);
			Integer sequenceInt = sequenceAttr.get();
			int sequence = 1;
			if (sequenceInt != null) {
				sequence = sequenceInt.intValue();
			}
			
			if (packet.getSequence() != sequence) {
				logger.warn("Packet is out of sequence: {} vs {}", packet.getSequence(), sequence);
				return;
			}
			
			sequenceAttr.set(++sequence);

			if (Packets.usesCrypto(CRYPTED_IDS, packet)) {
				Crypto.instancePacket().decrypt(packet.getPayload());
				if (pad != 0) {
					int writerIndex = packet.getPayload().writerIndex();
					packet.getPayload().writerIndex(writerIndex - pad);
				}
			}

			if (lengthPayload > 0) {
				logger.debug("In - Command {} - {} bytes", String.format("%04x", command), lengthPayload);
				logger.debug(() -> ByteBufUtil.hexDump(packet.getPayload()));
			} else {
				logger.debug("In - Command {}", () -> String.format("%04x", command));
			}

			if (buffer.readableBytes() > 0) {
				Util.moveReadableToStart(buffer);
			} else {
				buffer.clear();
			}
			
			ctx.fireChannelRead(packet);
		} catch (Exception e) {
			logger.error("Failed to decode packet.", e);
		} finally {
			Util.safeRelease((ByteBuf) msg);
			Util.safeRelease(packetBuffer);
		}
	}

}
