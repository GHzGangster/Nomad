package savemgo.nomad.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;

public class Util {

	public static final int KEY_XOR = 0x5a7085af;
//	public static final int KEY_XOR = 0x0;

	public static final byte[] KEY_HMAC = new byte[] { (byte) 0x5A, (byte) 0x37, (byte) 0x2F, (byte) 0x62, (byte) 0x69,
			(byte) 0x4A, (byte) 0x34, (byte) 0x36, (byte) 0x54, (byte) 0x7A, (byte) 0x47, (byte) 0x46, (byte) 0x2D,
			(byte) 0x38, (byte) 0x79, (byte) 0x78 };

	@SuppressWarnings({ "unchecked" })
	public static <T> T cast(Object obj) {
		return (T) obj;
	}

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

	public static void moveReadableToStart(ByteBuf buffer) {
		int length = buffer.readableBytes();
		int longCount = length >>> 3;
		int byteCount = length & 7;

		int index = 0;
		for (int i = longCount; i > 0; i--) {
			long l = buffer.readLong();
			buffer.setLong(index, l);
			index += 8;
		}

		for (int i = byteCount; i > 0; i--) {
			byte b = buffer.readByte();
			buffer.setByte(index, b);
			index++;
		}

		buffer.setIndex(0, length);
	}

	public static void padTo(int offset, ByteBuf buffer) {
		int remainder = offset - buffer.writerIndex();
		if (remainder > 0) {
			buffer.writeZero(remainder);
		}
	}

	public static String readString(ByteBuf buffer, int maxLength) {
		return Util.readString(buffer, maxLength, CharsetUtil.ISO_8859_1);
	}

	public static String readString(ByteBuf buffer, int maxLength, Charset charset) {
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
		String str = input.toString(charset);
		input.release();
		return str;
	}

	public static void releaseBuffer(ByteBuf bo) {
		if (bo != null) {
			try {
				bo.release();
			} catch (Exception e) {
				//
			}
		}
	}

	public static void releaseBuffers(AtomicReference<ByteBuf[]> abos) {
		ByteBuf[] bos = abos.get();
		Util.releaseBuffers(bos);
	}

	public static void releaseBuffers(ByteBuf[] bos) {
		if (bos != null) {
			for (ByteBuf bo : bos) {
				releaseBuffer(bo);
			}
		}
	}

	public static void writeString(String str, int length, ByteBuf buffer) {
		Util.writeString(str, length, buffer, CharsetUtil.ISO_8859_1);
	}

	public static void writeString(String str, int length, ByteBuf buffer, Charset charset) {
		CharsetEncoder ce = charset.newEncoder();
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

	/**
	 * For debugging only.
	 * 
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static ByteBuf readFile(File file) throws Exception {
		ByteBuf bb = null;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
			FileChannel fc = raf.getChannel();
			bb = PooledByteBufAllocator.DEFAULT.directBuffer((int) file.length());
			ByteBuffer buffer = ByteBuffer.allocate(0x1000);
			while (fc.read(buffer) > 0) {
				buffer.flip();
				bb.writeBytes(buffer);
				buffer.clear();
			}
		} catch (Exception e) {
			safeRelease(bb);
			throw e;
		} finally {
			safeClose(raf);
		}
		return bb;
	}

	public static void safeClose(RandomAccessFile file) {
		if (file != null) {
			try {
				file.close();
			} catch (Exception e) {
			}
		}
	}

	public static void safeRelease(ByteBuf buffer) {
		if (buffer != null && buffer.refCnt() > 0) {
			buffer.release(buffer.refCnt());
		}
	}

}
