package savemgo.nomad.packet;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import savemgo.nomad.Util;

public class Packet {

	private static final Logger logger = LogManager.getLogger(Packet.class.getSimpleName());

	private static final int ERROR_MASK = 0xDAB055 << 8;

	public static final int OFFSET_COMMAND = 0x0;
	public static final int OFFSET_PAYLOAD_LENGTH = 0x2;
	public static final int OFFSET_SEQUENCE = 0x4;
	public static final int OFFSET_CHECKSUM = 0x8;
	public static final int OFFSET_PAYLOAD = 0x18;

	public static final int MAX_PAYLOAD_LENGTH = 0x3ff;

	private static final byte[] CHECKSUM_BLANK = new byte[16];

	private ByteBuf header;
	private ByteBuf payload;

	public Packet(int command) {
		this.header = PooledByteBufAllocator.DEFAULT.directBuffer(24);
		this.payload = null;
		setCommand(command);
	}

	public Packet(int command, int error) {
		this(command, error, false);
	}
	
	public Packet(int command, int error, boolean dontMask) {
		this.header = PooledByteBufAllocator.DEFAULT.directBuffer(24);
		this.payload = PooledByteBufAllocator.DEFAULT.directBuffer(4);
		setCommand(command);
		if (!dontMask) {
			error |= ERROR_MASK;
		}
		this.payload.writeInt(error);
	}

	public Packet(int command, ByteBuf payload) {
		this.header = PooledByteBufAllocator.DEFAULT.directBuffer(24);
		this.payload = payload;
		setCommand(command);
	}

	public Packet(ByteBuf header, ByteBuf payload) {
		this.header = header;
		this.payload = payload;
	}

	public ByteBuf getHeader() {
		return header;
	}

	public short getCommand() {
		short result = 0;
		try {
			result = header.getShort(OFFSET_COMMAND);
		} catch (IndexOutOfBoundsException e) {
			//
		}
		return result;
	}

	public void setCommand(int value) {
		try {
			header.setShort(OFFSET_COMMAND, value);
		} catch (IndexOutOfBoundsException e) {
			//
		}
	}

	public short getPayloadLength() {
		short result = 0;
		try {
			result = header.getShort(OFFSET_PAYLOAD_LENGTH);
		} catch (IndexOutOfBoundsException e) {
			//
		}
		return result;
	}

	public void setPayloadLength(int value) {
		try {
			header.setShort(OFFSET_PAYLOAD_LENGTH, value);
		} catch (IndexOutOfBoundsException e) {
			//
		}
	}

	public int getSequence() {
		int result = 0;
		try {
			result = header.getShort(OFFSET_SEQUENCE);
		} catch (IndexOutOfBoundsException e) {
			//
		}
		return result;
	}

	public void setSequence(int value) {
		try {
			header.setInt(OFFSET_SEQUENCE, value);
		} catch (IndexOutOfBoundsException e) {
			//
		}
	}

	public ByteBuf getPayload() {
		return payload;
	}

	public void setPayload(ByteBuf payload) {
		this.payload = payload;
	}

	private byte[] getChecksum() {
		byte[] result = new byte[16];
		try {
			header.getBytes(OFFSET_CHECKSUM, result, 0, result.length);
		} catch (IndexOutOfBoundsException e) {
			//
		}
		return result;
	}

	private byte[] calculateChecksum() {
		byte[] result = CHECKSUM_BLANK;
		try {
			int payloadLength = getPayloadLength();
			byte[] bytes = new byte[8 + payloadLength];
			header.getBytes(0, bytes, 0, 8);
			if (payload != null) {
				payload.getBytes(0, bytes, 8, payloadLength);
			}

			SecretKeySpec keySpec = new SecretKeySpec(Util.KEY_HMAC, "HmacMD5");
			Mac mac = Mac.getInstance("HmacMD5");
			mac.init(keySpec);

			result = mac.doFinal(bytes);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			logger.error("Failed to calculte checksum.", e);
		}
		return result;
	}

	public void prepare() {
		if (payload == null) {
			setPayloadLength(0);
		} else {
			setPayloadLength(payload.capacity());
		}

		byte[] checksum = calculateChecksum();
		try {
			header.setBytes(OFFSET_CHECKSUM, checksum, 0, checksum.length);
		} catch (IndexOutOfBoundsException e) {
			//
		}

		header.setIndex(0, header.capacity());
		if (payload != null) {
			payload.setIndex(0, payload.capacity());
		}
	}

	public boolean validate() {
		if (getCommand() == 0) {
			return false;
		}

		byte[] actual = getChecksum();
		if (Arrays.equals(actual, CHECKSUM_BLANK)) {
			return false;
		}

		byte[] calculated = calculateChecksum();
		if (Arrays.equals(calculated, CHECKSUM_BLANK)) {
			return false;
		}

		return Arrays.equals(actual, calculated);
	}

	public void release() {
		header.release();
		if (payload != null) {
			payload.release();
		}
	}

}
