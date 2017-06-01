package savemgo.nomad.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

public class Crypto {

	private static Crypto instanceAuth = null, instancePacket = null;

	private final ByteBuf ck;

	private Crypto(byte[] k) {
		this.ck = Unpooled.wrappedBuffer(k);
	}

	public static Crypto instanceAuth() {
		if (instanceAuth == null) {
			instanceAuth = new Crypto(Constants.CRYPTO_AUTH);
		}
		return instanceAuth;
	}

	public static Crypto instancePacket() {
		if (instancePacket == null) {
			instancePacket = new Crypto(Constants.CRYPTO_PACKET);
		}
		return instancePacket;
	}

	public byte[] decrypt(byte[] bytes) {
		ByteBuf c = null, p = null;
		byte[] result = null;
		try {
			c = Unpooled.wrappedBuffer(bytes);
			p = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
			decrypt(p, c);
			result = new byte[bytes.length];
			p.getBytes(0, result);
		} finally {
			if (c != null) {
				c.release();
			}
			if (p != null) {
				p.release();
			}
		}
		return result;
	}

	public void decrypt(ByteBuf b) {
		decrypt(b, b);
	}

	public void decrypt(ByteBuf p, ByteBuf c) {
		int blocks = c.capacity() / 8;

		int b = 0, a = 0;
		int k = 0, kA = 0, kB = 0, kC = 0, kD = 0;
		int kIndexA = 0, kIndexB = 0, kIndexC = 0, kIndexD = 0;

		for (int i = 0; i < blocks; i++) {
			a = c.getInt(i * 8 + 4);
			b = c.getInt(i * 8);

			k = ck.getInt(0x44);
			b ^= k;

			for (int j = 0; j < 8; j++) {
				k = ck.getInt(0x40 - j * 8);
				a ^= k;

				kIndexA = (b >>> 22) & 0x3fc;
				kIndexB = (b >>> 14) & 0x3fc;
				kIndexC = (b >>> 6) & 0x3fc;
				kIndexD = Integer.rotateLeft(b, 2) & 0x3fc;

				kA = ck.getInt(kIndexA + 0x48);
				kB = ck.getInt(kIndexB + 0x448);
				kC = ck.getInt(kIndexC + 0x848);
				kD = ck.getInt(kIndexD + 0xc48);

				a ^= ((kA + kB) ^ kC) + kD;

				k = ck.getInt(0x3c - j * 8);
				b ^= k;

				kIndexA = (a >>> 22) & 0x3fc;
				kIndexB = (a >>> 14) & 0x3fc;
				kIndexC = (a >>> 6) & 0x3fc;
				kIndexD = Integer.rotateLeft(a, 2) & 0x3fc;

				kA = ck.getInt(kIndexA + 0x48);
				kB = ck.getInt(kIndexB + 0x448);
				kC = ck.getInt(kIndexC + 0x848);
				kD = ck.getInt(kIndexD + 0xc48);

				b ^= ((kA + kB) ^ kC) + kD;
			}

			k = ck.getInt(0x0);
			a ^= k;

			p.setInt(i * 8, a).setInt(i * 8 + 4, b);
		}
	}

	public byte[] encrypt(byte[] bytes) {
		ByteBuf c = null, p = null;
		byte[] result = null;
		try {
			c = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
			p = Unpooled.wrappedBuffer(bytes);
			encrypt(p, c);
			result = new byte[bytes.length];
			c.getBytes(0, result);
		} finally {
			if (c != null) {
				c.release();
			}
			if (p != null) {
				p.release();
			}
		}
		return result;
	}

	public void encrypt(ByteBuf b) {
		encrypt(b, b);
	}

	public void encrypt(ByteBuf p, ByteBuf c) {
		int blocks = p.capacity() / 8;

		int b = 0, a = 0;
		int k = 0, kA = 0, kB = 0, kC = 0, kD = 0;
		int kIndexA = 0, kIndexB = 0, kIndexC = 0, kIndexD = 0;

		for (int i = 0; i < blocks; i++) {
			a = p.getInt(i * 8 + 4);
			b = p.getInt(i * 8);

			k = ck.getInt(0x0);
			b ^= k;

			for (int j = 7; j >= 0; j--) {
				k = ck.getInt(0x3c - j * 8);
				a ^= k;

				kIndexA = (b >>> 22) & 0x3fc;
				kIndexB = (b >>> 14) & 0x3fc;
				kIndexC = (b >>> 6) & 0x3fc;
				kIndexD = Integer.rotateLeft(b, 2) & 0x3fc;

				kA = ck.getInt(kIndexA + 0x48);
				kB = ck.getInt(kIndexB + 0x448);
				kC = ck.getInt(kIndexC + 0x848);
				kD = ck.getInt(kIndexD + 0xc48);

				a ^= ((kA + kB) ^ kC) + kD;

				k = ck.getInt(0x40 - j * 8);
				b ^= k;

				kIndexA = (a >>> 22) & 0x3fc;
				kIndexB = (a >>> 14) & 0x3fc;
				kIndexC = (a >>> 6) & 0x3fc;
				kIndexD = Integer.rotateLeft(a, 2) & 0x3fc;

				kA = ck.getInt(kIndexA + 0x48);
				kB = ck.getInt(kIndexB + 0x448);
				kC = ck.getInt(kIndexC + 0x848);
				kD = ck.getInt(kIndexD + 0xc48);

				b ^= ((kA + kB) ^ kC) + kD;
			}

			k = ck.getInt(0x44);
			a ^= k;

			c.setInt(i * 8, a).setInt(i * 8 + 4, b);
		}
	}

}
