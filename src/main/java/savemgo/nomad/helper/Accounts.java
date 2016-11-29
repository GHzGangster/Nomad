package savemgo.nomad.helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.AttrUtil;
import savemgo.nomad.TypeUtil;
import savemgo.nomad.Util;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.crypto.KCrypt;
import savemgo.nomad.packet.Packet;

public class Accounts {

	private static final Logger logger = LogManager.getLogger(Accounts.class.getSimpleName());

	private static final byte[] XOR_SESSION_ID = new byte[] { (byte) 0x35, (byte) 0xd5, (byte) 0xc3, (byte) 0x8e,
			(byte) 0xd0, (byte) 0x11, (byte) 0x0e, (byte) 0xa8 };

	public static void checkSession(ChannelHandlerContext ctx, Packet in) {
		try {
			int length = in.getPayloadLength();
			logger.debug("Length: " + length);

			ByteBuf bi = in.getPayload();
			int id = bi.readInt();
			byte[] bytes = new byte[16];
			bi.readBytes(bytes);

			byte[] sessionId = decryptSessionId(bytes);
			String sessionIdStr = new String(sessionId, StandardCharsets.ISO_8859_1);

			int type = 0;

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("type", type);
			data.put("id", id);
			data.put("session", sessionIdStr);

			Map<String, Object> response = Campbell.instance().getResponse("auth", "checkSession", data);

			String result = TypeUtil.toString(response.get("result"));
			if (!result.equals("NOERR")) {
				logger.error("Error while checking session: " + result);
				ctx.write(new Packet(0x3004, 2));
				return;
			}

			AttrUtil.setSession(ctx, sessionIdStr);
			ctx.write(new Packet(0x3004, 0, true));
		} catch (Exception e) {
			logger.error("Exception while checking session.", e);
			ctx.write(new Packet(0x3004, 1));
		}
	}

	private static byte[] decryptSessionId(byte[] full) {
		byte[] bytes = new byte[8];
		System.arraycopy(full, 0, bytes, 0, bytes.length);
		Util.xor(bytes, XOR_SESSION_ID);
		return KCrypt.instance().encrypt(bytes);
	}

	private static final byte[] CHARLIST_END = new byte[] { (byte) 0x07, (byte) 0x00, (byte) 0x03 };

	public static void getCharacterList(ChannelHandlerContext ctx) {
		try {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("session", AttrUtil.getSession(ctx));

			Map<String, Object> response = Campbell.instance().getResponse("auth", "getCharacterList", data);

			String result = TypeUtil.toString(response.get("result"));
			if (!result.equals("NOERR")) {
				logger.error("Error while checking session: " + result);
				ctx.write(new Packet(0x3049, 2));
				return;
			}

			String bytes1Encoded = TypeUtil.toString(response.get("bytes1"));
			byte[] bytes1 = Base64.decodeBase64(bytes1Encoded);
			
			int slots = TypeUtil.toInt(response.get("slots"));
			ArrayList<LinkedTreeMap<String, Object>> chars = TypeUtil.cast(response.get("chars"));
			int numChars = Math.min(chars.size(), 8);
			
			ByteBuf bo = ctx.alloc().directBuffer(0x1d7);
			bo.writeInt(0).writeByte(slots).writeByte(numChars).writeZero(1);

			for (int i = 0; i < numChars; i++) {
				LinkedTreeMap<String, Object> chara = chars.get(i);
				
				int id = TypeUtil.toInt(chara.get("id"));
				String name = TypeUtil.toString(chara.get("name"));

				LinkedTreeMap<String, Object> appearance = TypeUtil.cast(chara.get("appearance"));
				int gender = TypeUtil.toInt(appearance.get("gender"));
				int face = TypeUtil.toInt(appearance.get("face"));
				int voice = TypeUtil.toInt(appearance.get("voice"));
				int pitch = TypeUtil.toInt(appearance.get("pitch"));
				int head = TypeUtil.toInt(appearance.get("head"));
				int headColor = TypeUtil.toInt(appearance.get("headColor"));
				int upper = TypeUtil.toInt(appearance.get("upper"));
				int upperColor = TypeUtil.toInt(appearance.get("upperColor"));
				int lower = TypeUtil.toInt(appearance.get("lower"));
				int lowerColor = TypeUtil.toInt(appearance.get("lowerColor"));
				int chest = TypeUtil.toInt(appearance.get("chest"));
				int chestColor = TypeUtil.toInt(appearance.get("chestColor"));
				int waist = TypeUtil.toInt(appearance.get("waist"));
				int waistColor = TypeUtil.toInt(appearance.get("waistColor"));
				int hands = TypeUtil.toInt(appearance.get("hands"));
				int handsColor = TypeUtil.toInt(appearance.get("handsColor"));
				int feet = TypeUtil.toInt(appearance.get("feet"));
				int feetColor = TypeUtil.toInt(appearance.get("feetColor"));
				int accessory1 = TypeUtil.toInt(appearance.get("accessory1"));
				int accessory1Color = TypeUtil.toInt(appearance.get("accessory1Color"));
				int accessory2 = TypeUtil.toInt(appearance.get("accessory2"));
				int accessory2Color = TypeUtil.toInt(appearance.get("accessory2Color"));
				int facePaint = TypeUtil.toInt(appearance.get("facePaint"));

				if (i == 0) {
					Util.writeString(name, 16, bo);
					bo.writeZero(1);
				} else {
					bo.writeInt(i);
				}
				bo.writeInt(id);
				Util.writeString(name, 16, bo);
				bo.writeByte(gender).writeByte(face).writeByte(upper).writeByte(lower).writeByte(facePaint)
						.writeByte(upperColor).writeByte(lowerColor).writeByte(voice).writeByte(pitch).writeZero(4)
						.writeByte(head).writeByte(chest).writeByte(hands).writeByte(waist).writeByte(feet)
						.writeByte(accessory1).writeByte(accessory2).writeByte(headColor).writeByte(chestColor)
						.writeByte(handsColor).writeByte(waistColor).writeByte(feetColor).writeByte(accessory1Color)
						.writeByte(accessory2Color).writeZero(1);
			}
			
			bo.writeZero(0x1b4 - bo.writerIndex());
			bo.writeBytes(bytes1);

			ctx.write(new Packet(0x3049, bo));
		} catch (Exception e) {
			logger.error("Exception while checking session.", e);
			ctx.write(new Packet(0x3049, 1));
		}
	}

	public static void createCharacter(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();

			String name = Util.readString(bi, 16);

			int gender = bi.readByte();
			int face = bi.readByte();
			int upper = bi.readByte();
			int lower = 0;
			bi.skipBytes(1);
			int facePaint = bi.readByte();
			int upperColor = bi.readByte();
			int lowerColor = bi.readByte();
			int voice = bi.readByte();
			int pitch = bi.readByte();
			bi.skipBytes(4);
			int head = bi.readByte();
			int chest = bi.readByte();
			int hands = bi.readByte();
			int waist = bi.readByte();
			int feet = bi.readByte();
			int accessory1 = bi.readByte();
			int accessory2 = bi.readByte();
			int headColor = bi.readByte();
			int chestColor = bi.readByte();
			int handsColor = 0;
			bi.skipBytes(1);
			int waistColor = bi.readByte();
			int feetColor = bi.readByte();
			int accessory1Color = bi.readByte();
			int accessory2Color = bi.readByte();

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("session", AttrUtil.getSession(ctx));
			data.put("name", name);
			data.put("gender", gender);
			data.put("face", face);
			data.put("voice", voice);
			data.put("pitch", pitch);
			data.put("head", head);
			data.put("headColor", headColor);
			data.put("upper", upper);
			data.put("upperColor", upperColor);
			data.put("lower", lower);
			data.put("lowerColor", lowerColor);
			data.put("chest", chest);
			data.put("chestColor", chestColor);
			data.put("waist", waist);
			data.put("waistColor", waistColor);
			data.put("hands", hands);
			data.put("handsColor", handsColor);
			data.put("feet", feet);
			data.put("feetColor", feetColor);
			data.put("accessory1", accessory1);
			data.put("accessory1Color", accessory1Color);
			data.put("accessory2", accessory2);
			data.put("accessory2Color", accessory2Color);
			data.put("facePaint", facePaint);

			Map<String, Object> response = Campbell.instance().getResponse("auth", "createCharacter", data);

			String result = TypeUtil.toString(response.get("result"));
			if (!result.equals("NOERR")) {
				logger.error("Error while creating character: " + result);
				ctx.write(new Packet(0x3102, 2));
				return;
			}

			ctx.write(new Packet(0x3102));
		} catch (Exception e) {
			logger.error("Exception while creating character.", e);
			ctx.write(new Packet(0x3102, 1));
		}
	}

	public static void selectCharacter(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("session", AttrUtil.getSession(ctx));
			data.put("index", index);

			Map<String, Object> response = Campbell.instance().getResponse("auth", "selectCharacter", data);

			String result = TypeUtil.toString(response.get("result"));
			if (!result.equals("NOERR")) {
				logger.error("Error while selecting character: " + result);
				ctx.write(new Packet(0x3104, 2));
				return;
			}

			ctx.write(new Packet(0x3104, 0, true));
		} catch (Exception e) {
			logger.error("Exception while selecting character.", e);
			ctx.write(new Packet(0x3104, 1));
		}
	}
	
	public static void deleteCharacter(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			Map<String, Object> data = new LinkedHashMap<>();
			data.put("session", AttrUtil.getSession(ctx));
			data.put("index", index);

			Map<String, Object> response = Campbell.instance().getResponse("auth", "deleteCharacter", data);

			String result = TypeUtil.toString(response.get("result"));
			if (!result.equals("NOERR")) {
				logger.error("Error while deleting character: " + result);
				ctx.write(new Packet(0x3106, 2));
				return;
			}

			ctx.write(new Packet(0x3106, 0, true));
		} catch (Exception e) {
			logger.error("Exception while deleting character.", e);
			ctx.write(new Packet(0x3106, 1));
		}
	}

}
