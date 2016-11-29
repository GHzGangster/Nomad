package savemgo.nomad;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;

public class Util {

	public static final int KEY_XOR = 0x5a7085af;

	public static final byte[] KEY_HMAC = new byte[] { (byte) 0x5A, (byte) 0x37, (byte) 0x2F, (byte) 0x62, (byte) 0x69,
			(byte) 0x4A, (byte) 0x34, (byte) 0x36, (byte) 0x54, (byte) 0x7A, (byte) 0x47, (byte) 0x46, (byte) 0x2D,
			(byte) 0x38, (byte) 0x79, (byte) 0x78 };

	public static byte[] intToBytes(int value) {
		return new byte[] { (byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8), (byte) value };
	}

	public static void xor(byte[] bytes, byte[] key) {
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] ^= key[i];
		}
	}
	
	public static void xor(ByteBuf buffer, int length, int key) {
		long keyLong = key & 0xffffffffL;
		keyLong |= (keyLong << 32);

		byte[] keyBytes = intToBytes(key);

		int longCount = length >>> 3;
		int byteCount = length & 7;

		int index = 0;

		for (int i = longCount; i > 0; i--) {
			long l = buffer.getLong(index) ^ keyLong;
			buffer.setLong(index, l);
			index += 8;
		}

		for (int i = byteCount; i > 0; i--) {
			byte b = (byte) (buffer.getByte(index) ^ keyBytes[index % 4]);
			buffer.setByte(index, b);
			index++;
		}
	}

	public static void writeString(String str, int length, ByteBuf buffer) {
		CharsetEncoder ce = CharsetUtil.ISO_8859_1.newEncoder();
		String newStr = str.substring(0, Math.min(str.length(), length));
		ByteBuffer niobuf = ByteBuffer.allocate(length);
		try {
			ce.encode(CharBuffer.wrap(newStr), niobuf, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		niobuf.position(0);
		buffer.writeBytes(niobuf);
	}

	public static String readString(ByteBuf buffer, int maxLength) {
		ByteBuf input = PooledByteBufAllocator.DEFAULT.buffer(maxLength);
		int len = (buffer.readableBytes() >= maxLength) ? maxLength : buffer.readableBytes();
		buffer.readBytes(input, len);
		for (int i = 0; i < len; i++) {
			byte b = input.getByte(i);
			if (b == (byte) 0x00) {
				input.capacity(i);
				break;
			}
		}
		String str = input.toString(CharsetUtil.ISO_8859_1);
		input.release();
		return str;
	}

	public static byte[] decodeBase64(String str) {
		if (str == null || str.length() <= 0) {
			return new byte[0];
		}
		ByteBuf bb = Unpooled.wrappedBuffer(str.getBytes());
		bb.readerIndex(0);
		ByteBuf bbstages = Base64.decode(bb);
		byte[] stages = new byte[bbstages.readableBytes()];
		bbstages.readBytes(stages, 0, stages.length);
		bb.release();
		bbstages.release();
		return stages;
	}

	public static String encodeBase64(byte[] bytes) {
		if (bytes == null || bytes.length <= 0) {
			return "";
		}
		ByteBuf bb = Unpooled.wrappedBuffer(bytes);
		ByteBuf bbe = Base64.encode(bb, false);
		String str = bbe.toString(CharsetUtil.ISO_8859_1);
		bb.release();
		bbe.release();
		return str;
	}

}
