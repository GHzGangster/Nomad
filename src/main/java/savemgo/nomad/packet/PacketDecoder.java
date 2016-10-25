package savemgo.nomad.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import savemgo.nomad.Util;

@Sharable
public class PacketDecoder extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf buffer = null, packetBuffer = null;
		try {
			buffer = (ByteBuf) msg;

			int readable = buffer.readableBytes();

			if (readable == 0) {
				System.out.println("No bytes to read in.");
				return;
			}

			if (readable < Packet.OFFSET_PAYLOAD) {
				System.out.println("Packet is too short, waiting for header...");
				return;
			}

			short lengthPayload = buffer.getShort(buffer.readerIndex() + 2);
			lengthPayload ^= Util.KEY_XOR & 0xffff;

			if (lengthPayload < 0 || Packet.MAX_PAYLOAD_LENGTH < lengthPayload) {
				System.out.println("Payload isn't a valid length.");
				return;
			}

			int bytesToRead = Packet.OFFSET_PAYLOAD + lengthPayload;

			if (readable < bytesToRead) {
				System.out.println("Packet is too short, waiting for payload...");
				return;
			}

			packetBuffer = ctx.alloc().directBuffer(bytesToRead);
			packetBuffer.writeBytes(buffer, bytesToRead);

			Util.xorBuffer(packetBuffer, packetBuffer.readableBytes(), Util.KEY_XOR);

			ByteBuf header = packetBuffer.copy(Packet.OFFSET_COMMAND, Packet.OFFSET_PAYLOAD);
			ByteBuf payload = packetBuffer.copy(Packet.OFFSET_PAYLOAD, lengthPayload);

			Packet packet = new Packet(header, payload);

			if (!packet.validate()) {
				System.out.println("Packet is invalid.");
				return;
			}

			System.out.println("In - Command " + Integer.toHexString(packet.getCommand()) + " - "
					+ packet.getPayloadLength() + " bytes");
			if (packet.getPayloadLength() > 0) {
				System.out.println("# " + ByteBufUtil.hexDump(packet.getPayload()));
			}

			System.out.println(ByteBufUtil.hexDump(packetBuffer));
			
			ctx.fireChannelRead(packet);
		} catch (Exception e) {
			System.err.println("Failed to decode packet.");
			e.printStackTrace();
		} finally {
			buffer.release();
			packetBuffer.release();
		}
	}

}
