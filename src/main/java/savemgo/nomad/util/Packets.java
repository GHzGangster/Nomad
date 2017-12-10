package savemgo.nomad.util;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.packet.Packet;

public class Packets {

	public static void flush(Channel ch) {
		ch.flush();
	}
	
	public static void flush(ChannelHandlerContext ctx) {
		flush(ctx.channel());
	}
	
	public static void handleMutliElementPayload(ChannelHandlerContext ctx, int elements, int maxElements,
			int bytesPerElement, AtomicReference<ByteBuf[]> payloadsRef, ElementConsumer consumer) throws Exception {
		int full = elements / maxElements;
		int partial = elements % maxElements;

		boolean hasPartial = partial > 0;

		int total = full + (hasPartial ? 1 : 0);

		payloadsRef.set(new ByteBuf[total]);
		ByteBuf[] payloads = payloadsRef.get();

		int elem = 0;
		for (int i = 0; i < total; i++) {
			int elems;
			if (!hasPartial || i < total - 1) {
				elems = maxElements;
			} else {
				elems = partial;
			}
			payloads[i] = ctx.alloc().directBuffer(elems * bytesPerElement);
			for (int j = 0; j < elems; j++) {
				consumer.accept(elem++, payloads[i]);
			}
		}
	}

	public static boolean usesCrypto(int[] commands, int command) {
		for (int cmd : commands) {
			if (command == cmd) {
				return true;
			}
		}
		return false;
	}

	public static boolean usesCrypto(int[] commands, Packet packet) {
		return usesCrypto(commands, packet.getCommand());
	}

	public static void write(Channel ch, int command) {
		ch.write(new Packet(command));
	}

	public static void write(ChannelHandlerContext ctx, int command) {
		write(ctx.channel(), command);
	}

	public static void write(Channel ch, int command, AtomicReference<ByteBuf[]> abos) {
		ByteBuf[] bos = abos.get();
		if (bos != null) {
			write(ch, command, bos);
		}
	}

	public static void write(ChannelHandlerContext ctx, int command, AtomicReference<ByteBuf[]> abos) {
		write(ctx.channel(), command, abos);
	}

	public static void write(Channel ch, int command, ByteBuf bo) {
		ch.write(new Packet(command, bo));
	}

	public static void write(ChannelHandlerContext ctx, int command, ByteBuf bo) {
		write(ctx.channel(), command, bo);
	}

	public static void write(Channel ch, int command, ByteBuf[] bos) {
		for (ByteBuf bo : bos) {
			write(ch, command, bo);
		}
	}

	public static void write(ChannelHandlerContext ctx, int command, ByteBuf[] bos) {
		write(ctx.channel(), command, bos);
	}

	public static void write(Channel ch, int command, int result) {
		ch.write(new Packet(command, result, true));
	}
	
	public static void write(ChannelHandlerContext ctx, int command, int result) {
		write(ctx.channel(), command, result);
	}

	public static void writeError(Channel ch, int command, int result) {
		ch.write(new Packet(command, result));
	}
	
	public static void writeError(ChannelHandlerContext ctx, int command, int result) {
		writeError(ctx.channel(), command, result);
	}
	
	public static void write(ChannelHandlerContext ctx, int command, Error error) {
		if (error.isOfficial()) {
			write(ctx.channel(), command, error.getCode());
		} else {
			writeError(ctx.channel(), command, error.getCode());
		}
	}

}
