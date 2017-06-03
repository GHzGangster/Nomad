package savemgo.nomad.helper;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.entity.CharacterBlocked;
import savemgo.nomad.entity.CharacterChatMacro;
import savemgo.nomad.entity.CharacterFriend;
import savemgo.nomad.entity.CharacterSkills;
import savemgo.nomad.entity.User;
import savemgo.nomad.instance.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Characters {

	private static final Logger logger = LogManager.getLogger(Characters.class);

	public static void getCharacterInfo(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting character info: No User.");
				Packets.writeError(ctx, 0x4101, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			int currentEpoch = (int) Instant.now().getEpochSecond();

			byte bytes1[] = { (byte) 0x16, (byte) 0xAE, (byte) 0x03, (byte) 0x38, (byte) 0x01, (byte) 0x3E, (byte) 0x01,
					(byte) 0x50 };
			byte bytes2[] = { (byte) 0x00, (byte) 0x97, (byte) 0xFD, (byte) 0xAB, (byte) 0xFC, (byte) 0xFF, (byte) 0xFF,
					(byte) 0x7B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xCE, (byte) 0x00 };
			int time1 = currentEpoch - 1, time2 = currentEpoch;

			bo = ctx.alloc().directBuffer(0x243);

			bo.writeInt(character.getId());
			Util.writeString(character.getName(), 16, bo);
			bo.writeBytes(bytes1).writeInt(character.getExp());

			bo.writeInt(time1).writeInt(time2).writeZero(1);

			for (CharacterFriend friend : character.getFriends()) {
				bo.writeInt(friend.getTargetId());
			}
			Util.padTo(0x129, bo);

			for (CharacterBlocked blocked : character.getBlocked()) {
				bo.writeInt(blocked.getTargetId());
			}
			Util.padTo(0x229, bo);

			bo.writeBytes(bytes2);

			Packets.write(ctx, 0x4101, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4101, 1);
		}
	}

	/**
	 * TODO: Figure out the payload in more detail, and/or just store the raw
	 * bytes
	 * 
	 * @param ctx
	 */
	public static void getGameplayOptions(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting gameplay options: No User.");
				Packets.writeError(ctx, 0x4120, 2);
				return;
			}

			String gameplayOptionsJson = user.getCurrentCharacter().getGameplayOptions();
			if (gameplayOptionsJson == null) {
				gameplayOptionsJson = "{\"names\":[\"Attacking\",\"Defensive\",\"Communication\",\"Acknowledgements\"]}";
			}

			JsonObject gameplayOptions = Util.jsonDecode(gameplayOptionsJson);

			JsonArray names = gameplayOptions.get("names").getAsJsonArray();

			byte bytes1[] = { (byte) 0x01, (byte) 0x40, (byte) 0x40, (byte) 0x44, (byte) 0x04, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x51,
					(byte) 0x55, (byte) 0x10, (byte) 0x02, (byte) 0x01, (byte) 0x10, (byte) 0x11, (byte) 0xB1,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x03, (byte) 0x04,
					(byte) 0x02, (byte) 0x0A, (byte) 0x0C, (byte) 0x0D, (byte) 0x0B, (byte) 0x0E, (byte) 0x10,
					(byte) 0x11, (byte) 0x0F, (byte) 0x05, (byte) 0x07, (byte) 0x08, (byte) 0x06 };
			byte bytes2[] = { (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x10, (byte) 0x11, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

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
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4120, 1);
		}
	}

	/**
	 * TODO: Update information
	 * 
	 * @param ctx
	 * @param in
	 */
	public static void updateGameplayOptions(ChannelHandlerContext ctx, Packet in) {
		try {
			Packets.write(ctx, 0x4111, 0);
		} catch (Exception e) {
			logger.error("Exception while updating gameplay options.", e);
			Packets.writeError(ctx, 0x4111, 1);
		}
	}

	/**
	 * TODO: Update information
	 * 
	 * @param ctx
	 * @param in
	 */
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
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting gameplay options: No User.");
				Packets.writeError(ctx, 0x4121, 2);
				return;
			}

			List<CharacterChatMacro> macros = user.getCurrentCharacter().getChatMacros();

			bos = new ByteBuf[2];
			for (int i = 0; i < bos.length; i++) {
				bos[i] = ctx.alloc().directBuffer(0x301);
				bos[i].writeByte(i);
			}

			for (CharacterChatMacro macro : macros) {
				Util.writeString(macro.getText(), 64, bos[macro.getType()]);
			}

			for (ByteBuf bo : bos) {
				Packets.write(ctx, 0x4121, bo);
			}
		} catch (Exception e) {
			logger.error("Exception while getting chat macros.", e);
			Util.releaseBuffers(bos);
			Packets.writeError(ctx, 0x4121, 1);
		}
	}

	public static void updateChatMacros(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting updating chat macros: No User.");
				Packets.writeError(ctx, 0x4115, 2);
				return;
			}

			ByteBuf bi = in.getPayload();
			final int type = bi.readByte();

			Character character = user.getCurrentCharacter();

			List<CharacterChatMacro> macros = character.getChatMacros();
			if (macros == null) {
				macros = new ArrayList<CharacterChatMacro>();
				character.setChatMacros(macros);
				for (int typem = 0; typem < 2; typem++) {
					for (int index = 0; index < 12; index++) {
						CharacterChatMacro macro = new CharacterChatMacro();
						macro.setCharacter(character);
						macro.setType(typem);
						macro.setIndex(index);
					}
				}
			}

			for (int i = 0; i < 12; i++) {
				final int index = i;
				CharacterChatMacro macro = macros.stream().filter((e) -> e.getIndex() == index && e.getType() == type)
						.findFirst().orElse(null);
				String text = Util.readString(bi, 64);
				macro.setText(text);
			}

			session = DB.getSession();
			session.beginTransaction();

			session.update(user);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4115, 0);
		} catch (Exception e) {
			logger.error("Exception while updating chat macros.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x4115, 1);
		}
	}

	public static void getPersonalInfo(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting personal details: No User.");
				Packets.writeError(ctx, 0x4122, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			CharacterAppearance appearance = character.getAppearance().get(0);
			CharacterSkills skills = character.getSkills().get(0);

			int clanId = 0;
			String clanName = "";
			int time1 = (int) Instant.now().getEpochSecond();
			int rwd = 1738;

			byte bytes1[] = { (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0C, (byte) 0x00, (byte) 0x01,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 };
			byte bytes3[] = { (byte) 0x03, (byte) 0x00, (byte) 0xA7, (byte) 0x00, (byte) 0x0D };

			bo = ctx.alloc().directBuffer(0xf5);

			bo.writeInt(clanId);
			Util.writeString(clanName, 16, bo);
			bo.writeBytes(bytes1).writeInt(time1);
			bo.writeByte(appearance.getGender()).writeByte(appearance.getFace()).writeByte(appearance.getUpper())
					.writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
					.writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
					.writeByte(appearance.getVoice()).writeByte(appearance.getPitch()).writeZero(4)
					.writeByte(appearance.getHead()).writeByte(appearance.getChest()).writeByte(appearance.getHands())
					.writeByte(appearance.getWaist()).writeByte(appearance.getFeet())
					.writeByte(appearance.getAccessory1()).writeByte(appearance.getAccessory2())
					.writeByte(appearance.getHeadColor()).writeByte(appearance.getChestColor())
					.writeByte(appearance.getHandsColor()).writeByte(appearance.getWaistColor())
					.writeByte(appearance.getFeetColor()).writeByte(appearance.getAccessory1Color())
					.writeByte(appearance.getAccessory2Color());

			bo.writeByte(skills.getSkill1()).writeByte(skills.getSkill2()).writeByte(skills.getSkill3())
					.writeByte(skills.getSkill4()).writeZero(1).writeByte(skills.getLevel1())
					.writeByte(skills.getLevel2()).writeByte(skills.getLevel3()).writeByte(skills.getLevel4())
					.writeZero(1);

			int skillExp = 0x600000;
			bo.writeByte(skillExp).writeByte(skillExp).writeByte(skillExp).writeByte(skillExp).writeZero(5);

			bo.writeInt(rwd);
			Util.writeString(character.getComment(), 128, bo);
			bo.writeInt(0).writeByte(character.getRank()).writeBytes(bytes3);

			Packets.write(ctx, 0x4122, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4122, 1);
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
			data.addProperty("session", "");
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
			data.addProperty("session", "");

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
			data.addProperty("session", "");

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
			data.addProperty("session", "");

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
			data.addProperty("session", "");
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
			data.addProperty("session", "");

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
			data.addProperty("session", "");
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
			data.addProperty("session", "");
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
			data.addProperty("session", "");
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
			data.addProperty("session", "");
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
			data.addProperty("session", "");
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
