package savemgo.nomad.helper;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.instance.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Characters {

	private static final Logger logger = LogManager.getLogger(Characters.class);

	public static void getCharacterInfo(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getCharacterInfo", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting character info: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4101, 2);
				return;
			}

			int id = response.get("id").getAsInt();
			String name = response.get("name").getAsString();
			int exp = response.get("exp").getAsInt();
			JsonArray friends = response.get("friends").getAsJsonArray();
			JsonArray blocked = response.get("blocked").getAsJsonArray();
			int time1 = response.get("time1").getAsInt();
			int time2 = response.get("time2").getAsInt();
			String bytes1Encoded = response.get("bytes1").getAsString();
			String bytes2Encoded = response.get("bytes2").getAsString();

			byte[] bytes1 = Base64.decodeBase64(bytes1Encoded);
			byte[] bytes2 = Base64.decodeBase64(bytes2Encoded);

			bo = ctx.alloc().directBuffer(0x243);

			bo.writeInt(id);
			Util.writeString(name, 16, bo);
			bo.writeBytes(bytes1).writeInt(exp);

			bo.writeInt(time1).writeInt(time2).writeZero(1);

			for (JsonElement elem : friends) {
				int cid = elem.getAsInt();
				bo.writeInt(cid);
			}
			Util.padTo(0x129, bo);

			for (JsonElement elem : blocked) {
				int cid = elem.getAsInt();
				bo.writeInt(cid);
			}
			Util.padTo(0x229, bo);

			bo.writeBytes(bytes2);

			Packets.write(ctx, 0x4101, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Packets.writeError(ctx, 0x4101, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void getGameplayOptions(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getGameplayOptions", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting gameplay settings: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4120, 2);
				return;
			}

			JsonArray names = response.get("names").getAsJsonArray();

			String bytes1Encoded = response.get("bytes1").getAsString();
			String bytes2Encoded = response.get("bytes2").getAsString();

			byte[] bytes1 = Base64.decodeBase64(bytes1Encoded);
			byte[] bytes2 = Base64.decodeBase64(bytes2Encoded);

			bo = ctx.alloc().directBuffer(0x150);

			bo.writeBytes(bytes1);

			for (JsonElement elem : names) {
				String name = elem.getAsString();
				Util.writeString(name, 64, bo);
			}

			bo.writeBytes(bytes2);

			Packets.write(ctx, 0x4120, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gameplay options.", e);
			Packets.writeError(ctx, 0x4120, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updateGameplayOptions(ChannelHandlerContext ctx, Packet in) {
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "updateGameplayOptions", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating gameplay options: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4111, 2);
				return;
			}

			Packets.write(ctx, 0x4111, 0);
		} catch (Exception e) {
			logger.error("Exception while updating gameplay options.", e);
			Packets.writeError(ctx, 0x4111, 1);
		}
	}

	public static void updateUiSettings(ChannelHandlerContext ctx, Packet in) {
		try {
			Packets.write(ctx, 0x4113, 0);
		} catch (Exception e) {
			logger.error("Exception while updating UI settings.", e);
			Packets.writeError(ctx, 0x4113, 1);
		}
	}

	public static void getChatMacros(ChannelHandlerContext ctx) {
		ByteBuf[] bos = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getChatMacros", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting chat macros: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4121, 2);
				return;
			}

			JsonArray normal = response.get("normal").getAsJsonArray();
			JsonArray shift = response.get("shift").getAsJsonArray();

			bos = new ByteBuf[2];
			for (int i = 0; i < bos.length; i++) {
				bos[i] = ctx.alloc().directBuffer(0x301);
				bos[i].writeByte(i);
			}

			for (int i = 0; i < 12; i++) {
				String norm = normal.get(i).getAsString();
				Util.writeString(norm, 64, bos[0]);

				String shft = shift.get(i).getAsString();
				Util.writeString(shft, 64, bos[1]);
			}

			for (ByteBuf bo : bos) {
				Packets.write(ctx, 0x4121, bo);
			}
		} catch (Exception e) {
			logger.error("Exception while getting chat macros.", e);
			Packets.writeError(ctx, 0x4121, 1);
			Util.releaseBuffers(bos);
		}
	}

	public static void updateChatMacros(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int type = bi.readByte();

			JsonArray macros = new JsonArray();
			for (int i = 0; i < 12; i++) {
				String text = Util.readString(bi, 64);
				macros.add(text);
			}

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("type", type);
			data.add("macros", macros);

			JsonObject response = Campbell.instance().getResponse("characters", "updateChatMacros", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating chat macros: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4115, 2);
				return;
			}

			Packets.write(ctx, 0x4115, 0);
		} catch (Exception e) {
			logger.error("Exception while updating chat macros.", e);
			Packets.writeError(ctx, 0x4115, 1);
		}
	}

	public static void getPersonalInfo(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getPersonalInfo", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting personal details: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4122, 2);
				return;
			}

			int clanId = response.get("clanId").getAsInt();
			String clanName = response.get("clanName").getAsString();
			int time1 = response.get("time1").getAsInt();
			JsonObject appearance = response.get("appearance").getAsJsonObject();
			JsonArray skills = response.get("skills").getAsJsonArray();
			String comment = response.get("comment").getAsString();
			int rwd = response.get("rwd").getAsInt();
			int rank = response.get("rank").getAsInt();
			String bytes1Encoded = response.get("bytes1").getAsString();
			String bytes3Encoded = response.get("bytes3").getAsString();

			int gender = appearance.get("gender").getAsInt();
			int face = appearance.get("face").getAsInt();
			int voice = appearance.get("voice").getAsInt();
			int pitch = appearance.get("pitch").getAsInt();
			int head = appearance.get("head").getAsInt();
			int headColor = appearance.get("headColor").getAsInt();
			int upper = appearance.get("upper").getAsInt();
			int upperColor = appearance.get("upperColor").getAsInt();
			int lower = appearance.get("lower").getAsInt();
			int lowerColor = appearance.get("lowerColor").getAsInt();
			int chest = appearance.get("chest").getAsInt();
			int chestColor = appearance.get("chestColor").getAsInt();
			int waist = appearance.get("waist").getAsInt();
			int waistColor = appearance.get("waistColor").getAsInt();
			int hands = appearance.get("hands").getAsInt();
			int handsColor = appearance.get("handsColor").getAsInt();
			int feet = appearance.get("feet").getAsInt();
			int feetColor = appearance.get("feetColor").getAsInt();
			int accessory1 = appearance.get("accessory1").getAsInt();
			int accessory1Color = appearance.get("accessory1Color").getAsInt();
			int accessory2 = appearance.get("accessory2").getAsInt();
			int accessory2Color = appearance.get("accessory2Color").getAsInt();
			int facePaint = appearance.get("facePaint").getAsInt();

			byte[] bytes1 = Base64.decodeBase64(bytes1Encoded);
			byte[] bytes3 = Base64.decodeBase64(bytes3Encoded);

			bo = ctx.alloc().directBuffer(0xf5);

			bo.writeInt(clanId);
			Util.writeString(clanName, 16, bo);
			bo.writeBytes(bytes1).writeInt(time1);
			bo.writeByte(gender).writeByte(face).writeByte(upper).writeByte(lower).writeByte(facePaint)
					.writeByte(upperColor).writeByte(lowerColor).writeByte(voice).writeByte(pitch).writeZero(4)
					.writeByte(head).writeByte(chest).writeByte(hands).writeByte(waist).writeByte(feet)
					.writeByte(accessory1).writeByte(accessory2).writeByte(headColor).writeByte(chestColor)
					.writeByte(handsColor).writeByte(waistColor).writeByte(feetColor).writeByte(accessory1Color)
					.writeByte(accessory2Color);

			for (int i = 0; i < 4; i++) {
				JsonObject skill = skills.get(i).getAsJsonObject();
				int id = skill.get("id").getAsInt();
				bo.writeByte(id);
			}
			bo.writeZero(1);

			for (int i = 0; i < 4; i++) {
				JsonObject skill = skills.get(i).getAsJsonObject();
				int level = skill.get("level").getAsInt();
				bo.writeByte(level);
			}
			bo.writeZero(1);

			for (int i = 0; i < 4; i++) {
				JsonObject skill = skills.get(i).getAsJsonObject();
				int exp = skill.get("exp").getAsInt();
				bo.writeInt(exp);
			}
			bo.writeZero(5);

			bo.writeInt(rwd);
			Util.writeString(comment, 128, bo);
			bo.writeInt(0).writeByte(rank).writeBytes(bytes3);

			Packets.write(ctx, 0x4122, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Packets.writeError(ctx, 0x4122, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updatePersonalInfo(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();

			int upper = bi.readUnsignedByte();
			int lower = bi.readUnsignedByte();
			int facePaint = bi.readUnsignedByte();
			int upperColor = bi.readUnsignedByte();
			int lowerColor = bi.readUnsignedByte();
			int head = bi.readUnsignedByte();
			int chest = bi.readUnsignedByte();
			int hands = bi.readUnsignedByte();
			int waist = bi.readUnsignedByte();
			int feet = bi.readUnsignedByte();
			int accessory1 = bi.readUnsignedByte();
			int accessory2 = bi.readUnsignedByte();
			int headColor = bi.readUnsignedByte();
			int chestColor = bi.readUnsignedByte();
			int handsColor = bi.readUnsignedByte();
			int waistColor = bi.readUnsignedByte();
			int feetColor = bi.readUnsignedByte();
			int accessory1Color = bi.readUnsignedByte();
			int accessory2Color = bi.readUnsignedByte();

			int skill1 = bi.readByte();
			int skill2 = bi.readByte();
			int skill3 = bi.readByte();
			int skill4 = bi.readByte();
			bi.skipBytes(1);
			int skill1Level = bi.readByte();
			int skill2Level = bi.readByte();
			int skill3Level = bi.readByte();
			int skill4Level = bi.readByte();

			bi.skipBytes(2);
			String comment = Util.readString(bi, 128);

			JsonObject appearance = new JsonObject();
			appearance.addProperty("head", head);
			appearance.addProperty("headColor", headColor);
			appearance.addProperty("upper", upper);
			appearance.addProperty("upperColor", upperColor);
			appearance.addProperty("lower", lower);
			appearance.addProperty("lowerColor", lowerColor);
			appearance.addProperty("chest", chest);
			appearance.addProperty("chestColor", chestColor);
			appearance.addProperty("waist", waist);
			appearance.addProperty("waistColor", waistColor);
			appearance.addProperty("hands", hands);
			appearance.addProperty("handsColor", handsColor);
			appearance.addProperty("feet", feet);
			appearance.addProperty("feetColor", feetColor);
			appearance.addProperty("accessory1", accessory1);
			appearance.addProperty("accessory1Color", accessory1Color);
			appearance.addProperty("accessory2", accessory2);
			appearance.addProperty("accessory2Color", accessory2Color);
			appearance.addProperty("facePaint", facePaint);

			JsonArray skills = new JsonArray();

			JsonObject skill = new JsonObject();
			skills.add(skill);
			skill.addProperty("skill", skill1);
			skill.addProperty("level", skill1Level);

			skill = new JsonObject();
			skills.add(skill);
			skill.addProperty("skill", skill2);
			skill.addProperty("level", skill2Level);

			skill = new JsonObject();
			skills.add(skill);
			skill.addProperty("skill", skill3);
			skill.addProperty("level", skill3Level);

			skill = new JsonObject();
			skills.add(skill);
			skill.addProperty("skill", skill4);
			skill.addProperty("level", skill4Level);

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.add("appearance", appearance);
			data.add("skills", skills);
			data.addProperty("comment", comment);

			JsonObject response = Campbell.instance().getResponse("characters", "updatePersonalInfo", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating personal info: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4131, 2);
				return;
			}

			skills = response.get("skills").getAsJsonArray();

			bo = ctx.alloc().directBuffer(0xba);

			bo.writeInt(0);
			bo.writeByte(upper).writeByte(lower).writeByte(facePaint).writeByte(upperColor).writeByte(lowerColor)
					.writeByte(head).writeByte(chest).writeByte(hands).writeByte(waist).writeByte(feet)
					.writeByte(accessory1).writeByte(accessory2).writeByte(headColor).writeByte(chestColor)
					.writeByte(handsColor).writeByte(waistColor).writeByte(feetColor).writeByte(accessory1Color)
					.writeByte(accessory2Color).writeByte(skill1).writeByte(skill2).writeByte(skill3).writeByte(skill4)
					.writeZero(1).writeByte(skill1Level).writeByte(skill2Level).writeByte(skill3Level)
					.writeByte(skill4Level).writeZero(1);

			for (JsonElement elem : skills) {
				JsonObject oskill = elem.getAsJsonObject();
				int exp = oskill.get("exp").getAsInt();
				bo.writeInt(exp);
			}
			bo.writeZero(5);

			Util.writeString(comment, 128, bo);

			Packets.write(ctx, 0x4131, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Packets.writeError(ctx, 0x4131, 1);
			Util.releaseBuffer(bo);
		}
	}

	private static final byte[] GEAR_TERMINATOR = new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
			(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

	public static void getGear(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getGear", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting gear: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4124, 2);
				return;
			}

			JsonArray gear = response.get("gear").getAsJsonArray();

			bo = ctx.alloc().directBuffer(0x24 + gear.size() * 0x5);

			bo.writeInt(gear.size());

			for (JsonElement elem : gear) {
				JsonObject item = elem.getAsJsonObject();
				int id = item.get("id").getAsInt();
				int colors = item.get("colors").getAsInt();
				bo.writeByte(id).writeInt(colors);
			}

			bo.writeBytes(GEAR_TERMINATOR);

			Packets.write(ctx, 0x4124, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gear.", e);
			Packets.writeError(ctx, 0x4124, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void getSkills(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getSkills", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting skills: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4125, 2);
				return;
			}

			JsonArray skills = response.get("skills").getAsJsonArray();

			bo = ctx.alloc().directBuffer(0x4 + skills.size() * 0x4);

			bo.writeInt(skills.size());

			for (JsonElement elem : skills) {
				JsonObject skill = elem.getAsJsonObject();
				int id = skill.get("id").getAsInt();
				int exp = skill.get("exp").getAsInt();
				bo.writeByte(id).writeShort(exp).writeZero(1);
			}

			Packets.write(ctx, 0x4125, bo);
		} catch (Exception e) {
			logger.error("Exception while getting skills.", e);
			Packets.writeError(ctx, 0x4125, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void getSkillSets(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getSkillSets", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting skill presets: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4140, 2);
				return;
			}

			JsonArray presets = response.get("presets").getAsJsonArray();

			bo = ctx.alloc().directBuffer(0x4d * presets.size());

			for (JsonElement elem : presets) {
				JsonObject preset = elem.getAsJsonObject();
				String name = preset.get("name").getAsString();
				int modes = preset.get("modes").getAsInt();
				JsonArray skills = preset.get("skills").getAsJsonArray();

				bo.writeInt(modes);

				for (int i = 0; i < 4; i++) {
					JsonObject skill = skills.get(i).getAsJsonObject();
					int id = skill.get("id").getAsInt();
					bo.writeByte(id);
				}
				bo.writeZero(1);
				for (int i = 0; i < 4; i++) {
					JsonObject skill = skills.get(i).getAsJsonObject();
					int level = skill.get("level").getAsInt();
					bo.writeByte(level);
				}
				bo.writeZero(1);

				Util.writeString(name, 63, bo, StandardCharsets.UTF_8);
			}

			Packets.write(ctx, 0x4140, bo);
		} catch (Exception e) {
			logger.error("Exception while getting skill presets.", e);
			Packets.writeError(ctx, 0x4140, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updateSkillSets(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();

			JsonArray skillSets = new JsonArray();

			for (int i = 0; i < 3; i++) {
				int modes = bi.readInt();
				int skill1 = bi.readByte();
				int skill2 = bi.readByte();
				int skill3 = bi.readByte();
				int skill4 = bi.readByte();
				bi.skipBytes(1);
				int level1 = bi.readByte();
				int level2 = bi.readByte();
				int level3 = bi.readByte();
				int level4 = bi.readByte();
				bi.skipBytes(1);
				String name = Util.readString(bi, 63, StandardCharsets.UTF_8);

				JsonObject skillSet = new JsonObject();
				skillSets.add(skillSet);
				skillSet.addProperty("name", name);
				skillSet.addProperty("modes", modes);

				JsonArray skills = new JsonArray();
				skillSet.add("skills", skills);

				JsonObject skill = new JsonObject();
				skills.add(skill);
				skill.addProperty("id", skill1);
				skill.addProperty("level", level1);

				skill = new JsonObject();
				skills.add(skill);
				skill.addProperty("id", skill2);
				skill.addProperty("level", level2);

				skill = new JsonObject();
				skills.add(skill);
				skill.addProperty("id", skill3);
				skill.addProperty("level", level3);

				skill = new JsonObject();
				skills.add(skill);
				skill.addProperty("id", skill4);
				skill.addProperty("level", level4);
			}

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.add("skillSets", skillSets);

			JsonObject response = Campbell.instance().getResponse("characters", "updateSkillSets", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating skill sets: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4141, 2);
				return;
			}

			Packets.write(ctx, 0x4141, 0);
		} catch (Exception e) {
			logger.error("Exception while updating skill sets.", e);
			Packets.writeError(ctx, 0x4141, 1);
		}
	}

	public static void getGearSets(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));

			JsonObject response = Campbell.instance().getResponse("characters", "getGearSets", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting gear presets: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4142, 2);
				return;
			}

			JsonArray presets = response.get("presets").getAsJsonArray();

			bo = ctx.alloc().directBuffer(0x57 * presets.size());

			for (JsonElement elem : presets) {
				JsonObject preset = elem.getAsJsonObject();
				String name = preset.get("name").getAsString();
				int stages = preset.get("stages").getAsInt();
				JsonObject gear = preset.get("gear").getAsJsonObject();

				int face = gear.get("face").getAsInt();
				int head = gear.get("head").getAsInt();
				int headColor = gear.get("headColor").getAsInt();
				int upper = gear.get("upper").getAsInt();
				int upperColor = gear.get("upperColor").getAsInt();
				int lower = gear.get("lower").getAsInt();
				int lowerColor = gear.get("lowerColor").getAsInt();
				int chest = gear.get("chest").getAsInt();
				int chestColor = gear.get("chestColor").getAsInt();
				int waist = gear.get("waist").getAsInt();
				int waistColor = gear.get("waistColor").getAsInt();
				int hands = gear.get("hands").getAsInt();
				int handsColor = gear.get("handsColor").getAsInt();
				int feet = gear.get("feet").getAsInt();
				int feetColor = gear.get("feetColor").getAsInt();
				int accessory1 = gear.get("accessory1").getAsInt();
				int accessory1Color = gear.get("accessory1Color").getAsInt();
				int accessory2 = gear.get("accessory2").getAsInt();
				int accessory2Color = gear.get("accessory2Color").getAsInt();
				int facePaint = gear.get("facePaint").getAsInt();

				bo.writeInt(stages);
				bo.writeByte(face).writeByte(head).writeByte(upper).writeByte(lower).writeByte(chest).writeByte(waist)
						.writeByte(hands).writeByte(feet).writeByte(accessory1).writeByte(accessory2)
						.writeByte(headColor).writeByte(upperColor).writeByte(lowerColor).writeByte(chestColor)
						.writeByte(waistColor).writeByte(handsColor).writeByte(feetColor).writeByte(accessory1Color)
						.writeByte(accessory2Color).writeByte(facePaint);
				Util.writeString(name, 63, bo, StandardCharsets.UTF_8);
			}

			Packets.write(ctx, 0x4142, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gear presets.", e);
			Packets.writeError(ctx, 0x4142, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updateGearSets(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();

			JsonArray gearSets = new JsonArray();

			for (int i = 0; i < 3; i++) {
				int stages = bi.readInt();
				int face = bi.readUnsignedByte();
				int head = bi.readUnsignedByte();
				int upper = bi.readUnsignedByte();
				int lower = bi.readUnsignedByte();
				int chest = bi.readUnsignedByte();
				int waist = bi.readUnsignedByte();
				int hands = bi.readUnsignedByte();
				int feet = bi.readUnsignedByte();
				int accessory1 = bi.readUnsignedByte();
				int accessory2 = bi.readUnsignedByte();
				int headColor = bi.readUnsignedByte();
				int upperColor = bi.readUnsignedByte();
				int lowerColor = bi.readUnsignedByte();
				int chestColor = bi.readUnsignedByte();
				int waistColor = bi.readUnsignedByte();
				int handsColor = bi.readUnsignedByte();
				int feetColor = bi.readUnsignedByte();
				int accessory1Color = bi.readUnsignedByte();
				int accessory2Color = bi.readUnsignedByte();
				int facePaint = bi.readUnsignedByte();
				String name = Util.readString(bi, 63, StandardCharsets.UTF_8);

				JsonObject gearSet = new JsonObject();
				gearSets.add(gearSet);
				gearSet.addProperty("name", name);
				gearSet.addProperty("stages", stages);

				JsonObject gear = new JsonObject();
				gearSet.add("gear", gear);
				gear.addProperty("face", face);
				gear.addProperty("head", head);
				gear.addProperty("headColor", headColor);
				gear.addProperty("upper", upper);
				gear.addProperty("upperColor", upperColor);
				gear.addProperty("lower", lower);
				gear.addProperty("lowerColor", lowerColor);
				gear.addProperty("chest", chest);
				gear.addProperty("chestColor", chestColor);
				gear.addProperty("waist", waist);
				gear.addProperty("waistColor", waistColor);
				gear.addProperty("hands", hands);
				gear.addProperty("handsColor", handsColor);
				gear.addProperty("feet", feet);
				gear.addProperty("feetColor", feetColor);
				gear.addProperty("accessory1", accessory1);
				gear.addProperty("accessory1Color", accessory1Color);
				gear.addProperty("accessory2", accessory2);
				gear.addProperty("accessory2Color", accessory2Color);
				gear.addProperty("facePaint", facePaint);
			}

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.add("gearSets", gearSets);

			JsonObject response = Campbell.instance().getResponse("characters", "updateGearSets", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating gear sets: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4143, 2);
				return;
			}

			Packets.write(ctx, 0x4143, 0);
		} catch (Exception e) {
			logger.error("Exception while updating gear sets.", e);
			Packets.writeError(ctx, 0x4143, 1);
		}
	}

	public static void updateConnectionInfo(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int privatePort = bi.readUnsignedShort();
			String privateIp = Util.readString(bi, 16);
			int publicPort = bi.readUnsignedShort();
			@SuppressWarnings("unused")
			short unk = bi.readShort();

			String publicIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("publicIp", publicIp);
			data.addProperty("publicPort", publicPort);
			data.addProperty("privateIp", privateIp);
			data.addProperty("privatePort", privatePort);

			JsonObject response = Campbell.instance().getResponse("characters", "updateConnectionInfo", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating connection info: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4701, 2);
				return;
			}

			Packets.write(ctx, 0x4701, 0);
		} catch (Exception e) {
			logger.error("Exception while updating connection info.", e);
			Packets.writeError(ctx, 0x4701, 1);
		}
	}

	public static void getMail(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4821, 0);
		Packets.write(ctx, 0x4823, 0);
	}

	public static void getFriendsBlockedList(ChannelHandlerContext ctx, Packet in) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			ByteBuf bi = in.getPayload();
			int type = bi.readByte();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("type", type);

			JsonObject response = Campbell.instance().getResponse("characters", "getFriendsBlockedList", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting friends/blocked list: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4581, 2);
				return;
			}

			JsonArray charas = response.get("charas").getAsJsonArray();

			Packets.handleMutliElementPayload(ctx, charas.size(), 23, 0x2b, payloads, (i, bo) -> {
				JsonObject chara = charas.get(i).getAsJsonObject();

				int playerId = chara.get("id").getAsInt();
				String playerName = chara.get("name").getAsString();

				JsonObject lobby = chara.get("lobby").getAsJsonObject();
				int lobbyId = lobby.get("id").getAsInt();

				JsonObject game = chara.get("game").getAsJsonObject();
				int gameId = game.get("id").getAsInt();
				String gameHostName = game.get("hostName").getAsString();
				int gameType = game.get("type").getAsInt();

				bo.writeInt(playerId);
				Util.writeString(playerName, 16, bo);
				bo.writeShort(lobbyId).writeInt(gameId);
				Util.writeString(gameHostName, 16, bo);
				bo.writeByte(gameType);
			});

			Packets.write(ctx, 0x4581, 0);
			Packets.write(ctx, 0x4582, payloads);
			Packets.write(ctx, 0x4583, 0);
		} catch (Exception e) {
			logger.error("Exception while getting friends/blocked list.", e);
			Packets.writeError(ctx, 0x4581, 1);
			Util.releaseBuffers(payloads);
		}
	}

	public static void getMatchHistory(ChannelHandlerContext ctx, Packet in) {
		try {
			// ByteBuf bo = Util.readFile(new File("match-history.bin"));
			Packets.write(ctx, 0x4681, 0);
			// Packets.write(ctx, 0x4682, bo);
			Packets.write(ctx, 0x4683, 0);
		} catch (Exception e) {
			logger.error("Exception while getting match history.", e);
			Packets.writeError(ctx, 0x4681, 1);
		}
	}

	public static void getPersonalStats(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bo1 = Util.readFile(new File("personal-stats-1.bin"));
			ByteBuf bo2 = Util.readFile(new File("personal-stats-2.bin"));
			ByteBuf bo3 = Util.readFile(new File("personal-stats-3.bin"));
			Packets.write(ctx, 0x4103, bo1);
			Packets.write(ctx, 0x4105, bo2);
			Packets.write(ctx, 0x4107, bo3);
		} catch (Exception e) {
			logger.error("Exception while getting personal stats.", e);
			Packets.writeError(ctx, 0x4103, 1);
		}
	}

	public static void addFriendsBlocked(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int type = bi.readByte();
			int target = bi.readInt();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("type", type);
			data.addProperty("target", target);

			JsonObject response = Campbell.instance().getResponse("characters", "addFriendsBlocked", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while adding to friends/blocked list: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4502, 2);
				return;
			}

			bo = ctx.alloc().directBuffer(0x19);
			bo.writeInt(0).writeInt(target).writeByte(type);
			Util.writeString("president trump", 16, bo);

			Packets.write(ctx, 0x4502, bo);
		} catch (Exception e) {
			logger.error("Exception while adding to friends/blocked list.", e);
			Packets.writeError(ctx, 0x4502, 1);
			Util.safeRelease(bo);
		}
	}

	public static void removeFriendsBlocked(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int type = bi.readByte();
			int target = bi.readInt();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("type", type);
			data.addProperty("target", target);

			JsonObject response = Campbell.instance().getResponse("characters", "removeFriendsBlocked", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while removing from friends/blocked list: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4512, 2);
				return;
			}

			bo = ctx.alloc().directBuffer(0x9);
			bo.writeInt(0).writeByte(type).writeInt(target);

			Packets.write(ctx, 0x4512, bo);
		} catch (Exception e) {
			logger.error("Exception while removing from friends/blocked list.", e);
			Packets.writeError(ctx, 0x4512, 1);
			Util.safeRelease(bo);
		}
	}

	public static void search(ChannelHandlerContext ctx, Packet in) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			ByteBuf bi = in.getPayload();
			boolean exactOnly = bi.readBoolean();
			boolean caseSensitive = bi.readBoolean();
			String name = Util.readString(bi, 0x10);

			JsonObject data = new JsonObject();
			data.addProperty("name", name);
			data.addProperty("exactOnly", exactOnly);
			data.addProperty("caseSensitive", caseSensitive);

			JsonObject response = Campbell.instance().getResponse("characters", "searchPlayer", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while searching for player: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4601, 2);
				return;
			}

			JsonArray charas = response.get("charas").getAsJsonArray();

			Packets.handleMutliElementPayload(ctx, charas.size(), 17, 0x3b, payloads, (i, bo) -> {
				JsonObject chara = charas.get(i).getAsJsonObject();

				int playerId = chara.get("id").getAsInt();
				String playerName = chara.get("name").getAsString();

				JsonObject lobby = chara.get("lobby").getAsJsonObject();
				int lobbyId = lobby.get("id").getAsInt();
				String lobbyName = lobby.get("name").getAsString();

				JsonObject game = chara.get("game").getAsJsonObject();
				int gameId = game.get("id").getAsInt();
				String gameHostName = game.get("hostName").getAsString();
				int gameType = game.get("type").getAsInt();

				bo.writeInt(playerId);
				Util.writeString(playerName, 16, bo);
				bo.writeShort(lobbyId);
				Util.writeString(lobbyName, 16, bo);
				bo.writeInt(gameId);
				Util.writeString(gameHostName, 16, bo);
				bo.writeByte(gameType);
			});

			Packets.write(ctx, 0x4601, 0);
			Packets.write(ctx, 0x4602, payloads);
			Packets.write(ctx, 0x4603, 0);
		} catch (Exception e) {
			logger.error("Exception while searching for player.", e);
			Packets.writeError(ctx, 0x4601, 1);
			Util.releaseBuffers(payloads);
		}
	}

	public static void getCharacterOverview(ChannelHandlerContext ctx, Packet in) {
		// In: 00000002 - Player ID
		try {
			ByteBuf bo1 = Util.readFile(new File("player-overview.bin"));
			Packets.write(ctx, 0x4221, bo1);
		} catch (Exception e) {
			logger.error("Exception while getting character overview.", e);
			Packets.writeError(ctx, 0x4221, 1);
		}
	}

	public static void getOfficialGameHistory(ChannelHandlerContext ctx, Packet in) {
		// In: 00000001 - Player ID
		try {
			Packets.write(ctx, 0x4685, 0);
			Packets.write(ctx, 0x4687, 0);
		} catch (Exception e) {
			logger.error("Exception while getting official game history.", e);
			Packets.writeError(ctx, 0x4685, 1);
		}
	}

}
