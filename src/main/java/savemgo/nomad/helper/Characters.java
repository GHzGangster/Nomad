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
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.entity.CharacterBlocked;
import savemgo.nomad.entity.CharacterChatMacro;
import savemgo.nomad.entity.CharacterEquippedSkills;
import savemgo.nomad.entity.CharacterFriend;
import savemgo.nomad.entity.CharacterSetGear;
import savemgo.nomad.entity.CharacterSetSkills;
import savemgo.nomad.entity.ConnectionInfo;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NUsers;
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

	public static void getGameplayOptionsUiSettings(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting gameplay options/ui settings: No User.");
				Packets.writeError(ctx, 0x4120, 2);
				return;
			}

			String json = user.getCurrentCharacter().getGameplayOptions();
			if (json == null) {
				json = "{\"onlineStatusMode\":0,\"emailFriendsOnly\":false,\"receiveNotices\":true,\"receiveInvites\":true,\"normalViewVerticalInvert\":false,\"normalViewHorizontalInvert\":false,\"normalViewSpeed\":5,\"shoulderViewVerticalInvert\":false,\"shoulderViewHorizontalInvert\":false,\"shoulderViewSpeed\":5,\"firstViewVerticalInvert\":false,\"firstViewHorizontalInvert\":false,\"firstViewSpeed\":5,\"firstViewPlayerDirection\":true,\"viewChangeSpeed\":5,\"firstViewMemory\":false,\"radarLockNorth\":false,\"radarFloorHide\":false,\"hudDisplaySize\":0,\"hudHideNameTags\":false,\"lockOnEnabled\":false,\"weaponSwitchMode\":2,\"weaponSwitchA\":0,\"weaponSwitchB\":1,\"weaponSwitchC\":2,\"itemSwitchMode\":2,\"codec1Name\":\"\",\"codec1a\":1,\"codec1b\":3,\"codec1c\":4,\"codec1d\":2,\"codec2Name\":\"\",\"codec2a\":10,\"codec2b\":12,\"codec2c\":13,\"codec2d\":11,\"codec3Name\":\"\",\"codec3a\":14,\"codec3b\":16,\"codec3c\":17,\"codec3d\":15,\"codec4Name\":\"\",\"codec4a\":5,\"codec4b\":7,\"codec4c\":8,\"codec4d\":6,\"voiceChatRecognitionLevel\":5,\"voiceChatVolume\":5,\"headsetVolume\":5,\"bgmVolume\":10}";
			}

			JsonObject data = Util.jsonDecode(json);

			int onlineStatusMode = data.get("onlineStatusMode").getAsInt();
			boolean emailFriendsOnly = data.get("emailFriendsOnly").getAsBoolean();
			boolean receiveNotices = data.get("receiveNotices").getAsBoolean();
			boolean receiveInvites = data.get("receiveInvites").getAsBoolean();

			boolean normalViewVerticalInvert = data.get("normalViewVerticalInvert").getAsBoolean();
			boolean normalViewHorizontalInvert = data.get("normalViewHorizontalInvert").getAsBoolean();
			int normalViewSpeed = data.get("normalViewSpeed").getAsInt();
			boolean shoulderViewVerticalInvert = data.get("shoulderViewVerticalInvert").getAsBoolean();
			boolean shoulderViewHorizontalInvert = data.get("shoulderViewHorizontalInvert").getAsBoolean();
			int shoulderViewSpeed = data.get("shoulderViewSpeed").getAsInt();
			boolean firstViewVerticalInvert = data.get("firstViewVerticalInvert").getAsBoolean();
			boolean firstViewHorizontalInvert = data.get("firstViewHorizontalInvert").getAsBoolean();
			int firstViewSpeed = data.get("firstViewSpeed").getAsInt();

			boolean firstViewPlayerDirection = data.get("firstViewPlayerDirection").getAsBoolean();
			int viewChangeSpeed = data.get("viewChangeSpeed").getAsInt();
			boolean firstViewMemory = data.get("firstViewMemory").getAsBoolean();
			boolean radarLockNorth = data.get("radarLockNorth").getAsBoolean();
			boolean radarFloorHide = data.get("radarFloorHide").getAsBoolean();
			int hudDisplaySize = data.get("hudDisplaySize").getAsInt();
			boolean hudHideNameTags = data.get("hudHideNameTags").getAsBoolean();
			boolean lockOnEnabled = data.get("lockOnEnabled").getAsBoolean();

			int weaponSwitchMode = data.get("weaponSwitchMode").getAsInt();
			int weaponSwitchA = data.get("weaponSwitchA").getAsInt();
			int weaponSwitchB = data.get("weaponSwitchB").getAsInt();
			int weaponSwitchC = data.get("weaponSwitchC").getAsInt();

			int itemSwitchMode = data.get("itemSwitchMode").getAsInt();

			String codec1Name = data.get("codec1Name").getAsString();
			int codec1a = data.get("codec1a").getAsInt();
			int codec1b = data.get("codec1b").getAsInt();
			int codec1c = data.get("codec1c").getAsInt();
			int codec1d = data.get("codec1d").getAsInt();

			String codec2Name = data.get("codec2Name").getAsString();
			int codec2a = data.get("codec2a").getAsInt();
			int codec2b = data.get("codec2b").getAsInt();
			int codec2c = data.get("codec2c").getAsInt();
			int codec2d = data.get("codec2d").getAsInt();

			String codec3Name = data.get("codec3Name").getAsString();
			int codec3a = data.get("codec3a").getAsInt();
			int codec3b = data.get("codec3b").getAsInt();
			int codec3c = data.get("codec3c").getAsInt();
			int codec3d = data.get("codec3d").getAsInt();

			String codec4Name = data.get("codec4Name").getAsString();
			int codec4a = data.get("codec4a").getAsInt();
			int codec4b = data.get("codec4b").getAsInt();
			int codec4c = data.get("codec4c").getAsInt();
			int codec4d = data.get("codec4d").getAsInt();

			int voiceChatRecognitionLevel = data.get("voiceChatRecognitionLevel").getAsInt();
			int voiceChatVolume = data.get("voiceChatVolume").getAsInt();
			int headsetVolume = data.get("headsetVolume").getAsInt();
			int bgmVolume = data.get("bgmVolume").getAsInt();

			viewChangeSpeed -= 1;

			int unknown = 1;

			int privacyA = 1;
			privacyA |= (onlineStatusMode & 0b11) << 4;
			privacyA |= emailFriendsOnly ? 0b01000000 : 0;

			int privacyB = 0;
			privacyB |= receiveNotices ? 0b1 : 0;
			privacyB |= receiveInvites ? 0b10000 : 0;

			int normalView = 0;
			normalView |= normalViewVerticalInvert ? 0b1 : 0;
			normalView |= normalViewHorizontalInvert ? 0b10 : 0;
			normalViewSpeed -= 1;
			normalView |= (normalViewSpeed & 0b1111) << 4;

			int shoulderView = 0;
			shoulderView |= shoulderViewVerticalInvert ? 0b1 : 0;
			shoulderView |= shoulderViewHorizontalInvert ? 0b10 : 0;
			shoulderViewSpeed -= 1;
			shoulderView |= (shoulderViewSpeed & 0b1111) << 4;

			int firstView = 0;
			firstView |= firstViewVerticalInvert ? 0b1 : 0;
			firstView |= firstViewHorizontalInvert ? 0b10 : 0;
			firstViewSpeed -= 1;
			firstView |= (firstViewSpeed & 0b1111) << 4;
			firstView |= firstViewPlayerDirection ? 0b100 : 0;

			byte _firstViewMemory = 0;
			_firstViewMemory |= firstViewMemory ? 0b10 : 0;

			int radar = 0;
			radar |= radarLockNorth ? 0b1 : 0;
			radar |= radarFloorHide ? 0b10000 : 0;

			int hudDisplay = 0;
			hudDisplay |= hudDisplaySize & 0b11;
			hudDisplay |= hudHideNameTags ? 0b10000 : 0;

			int lockOnAndBGM = 0;
			lockOnAndBGM |= lockOnEnabled ? 0b1 : 0;
			bgmVolume += 1;
			lockOnAndBGM |= (bgmVolume & 0b1111) << 4;

			int _weaponSwitchA = 0;
			_weaponSwitchA |= weaponSwitchA & 0b1111;
			_weaponSwitchA |= (weaponSwitchB & 0b1111) << 4;

			int _weaponSwitchB = 0;
			_weaponSwitchB |= weaponSwitchC & 0b1111;

			int switchModes = 0;
			switchModes |= weaponSwitchMode & 0b1111;
			switchModes |= (itemSwitchMode & 0b1111) << 4;

			int voiceChatA = 1;
			voiceChatA |= (voiceChatRecognitionLevel & 0b1111) << 4;

			int voiceChatB = 0;
			voiceChatB |= voiceChatVolume & 0b1111;
			voiceChatB |= (headsetVolume & 0b1111) << 4;

			bo = ctx.alloc().directBuffer(0x150);

			bo.writeByte(privacyA).writeByte(normalView).writeByte(shoulderView).writeByte(firstView)
					.writeByte(viewChangeSpeed).writeZero(6).writeByte(switchModes).writeZero(1).writeByte(voiceChatA)
					.writeByte(voiceChatB).writeByte(_weaponSwitchA).writeByte(_weaponSwitchB).writeByte(unknown)
					.writeByte(_firstViewMemory).writeByte(privacyB).writeByte(lockOnAndBGM).writeByte(radar)
					.writeByte(hudDisplay).writeZero(9).writeByte(codec1a).writeByte(codec1b).writeByte(codec1c)
					.writeByte(codec1d).writeByte(codec2a).writeByte(codec2b).writeByte(codec2c).writeByte(codec2d)
					.writeByte(codec3a).writeByte(codec3b).writeByte(codec3c).writeByte(codec3d).writeByte(codec4a)
					.writeByte(codec4b).writeByte(codec4c).writeByte(codec4d);
			Util.writeString(codec1Name, 64, bo);
			Util.writeString(codec2Name, 64, bo);
			Util.writeString(codec3Name, 64, bo);
			Util.writeString(codec4Name, 64, bo);
			bo.writeBytes(BYTES_GAMEPLAY_UI_SETTINGS);

			Packets.write(ctx, 0x4120, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gameplay options/ui settings.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4120, 1);
		}
	}

	private static final byte BYTES_GAMEPLAY_UI_SETTINGS[] = { (byte) 0x01, (byte) 0x00, (byte) 0x10, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x11, (byte) 0x10, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

	public static void updateGameplayOptions(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating gameplay options: No User.");
				Packets.writeError(ctx, 0x4111, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			ByteBuf bi = in.getPayload();
			byte privacyA = bi.readByte();
			byte normalView = bi.readByte();
			byte shoulderView = bi.readByte();
			byte firstView = bi.readByte();
			byte viewChangeSpeed = bi.readByte();
			bi.skipBytes(6);
			byte switchModes = bi.readByte();
			bi.skipBytes(1);
			byte voiceChatA = bi.readByte();
			byte voiceChatB = bi.readByte();
			byte _weaponSwitchA = bi.readByte();
			byte _weaponSwitchB = bi.readByte();
			bi.skipBytes(1);
			byte _firstViewMemory = bi.readByte();
			byte privacyB = bi.readByte();
			byte lockOnAndBGM = bi.readByte();
			byte radar = bi.readByte();
			byte hudDisplay = bi.readByte();
			bi.skipBytes(9);
			byte codec1a = bi.readByte();
			byte codec1b = bi.readByte();
			byte codec1c = bi.readByte();
			byte codec1d = bi.readByte();
			byte codec2a = bi.readByte();
			byte codec2b = bi.readByte();
			byte codec2c = bi.readByte();
			byte codec2d = bi.readByte();
			byte codec3a = bi.readByte();
			byte codec3b = bi.readByte();
			byte codec3c = bi.readByte();
			byte codec3d = bi.readByte();
			byte codec4a = bi.readByte();
			byte codec4b = bi.readByte();
			byte codec4c = bi.readByte();
			byte codec4d = bi.readByte();
			String codec1Name = Util.readString(bi, 64);
			String codec2Name = Util.readString(bi, 64);
			String codec3Name = Util.readString(bi, 64);
			String codec4Name = Util.readString(bi, 64);

			viewChangeSpeed += 1;

			int onlineStatusMode = (privacyA >> 4) & 0b11;
			boolean emailFriendsOnly = (privacyA & 0b01000000) == 0b01000000;

			boolean receiveNotices = (privacyB & 0b1) == 0b1;
			boolean receiveInvites = (privacyB & 0b10000) == 0b10000;

			boolean normalViewVerticalInvert = (normalView & 0b1) == 0b1;
			boolean normalViewHorizontalInvert = (normalView & 0b10) == 0b10;
			int normalViewSpeed = (normalView >> 4) & 0b1111;
			normalViewSpeed += 1;

			boolean shoulderViewVerticalInvert = (shoulderView & 0b1) == 0b1;
			boolean shoulderViewHorizontalInvert = (shoulderView & 0b10) == 0b10;
			int shoulderViewSpeed = (shoulderView >> 4) & 0b1111;
			shoulderViewSpeed += 1;

			boolean firstViewVerticalInvert = (firstView & 0b1) == 0b1;
			boolean firstViewHorizontalInvert = (firstView & 0b10) == 0b10;
			int firstViewSpeed = (firstView >> 4) & 0b1111;
			firstViewSpeed += 1;
			boolean firstViewPlayerDirection = (firstView & 0b100) == 0b100;
			boolean firstViewMemory = (_firstViewMemory & 0b10) == 0b10;

			boolean radarLockNorth = (radar & 0b1) == 0b1;
			boolean radarFloorHide = (radar & 0b10000) == 0b10000;

			int hudDisplaySize = hudDisplay & 0b11;
			boolean hudHideNameTags = (hudDisplay & 0b10000) == 0b10000;

			boolean lockOnEnabled = (lockOnAndBGM & 0b1) == 0b1;
			int bgmVolume = (lockOnAndBGM >> 4) & 0b1111;
			bgmVolume -= 1;

			int weaponSwitchA = _weaponSwitchA & 0b1111;
			int weaponSwitchB = (_weaponSwitchA >> 4) & 0b1111;

			int weaponSwitchC = _weaponSwitchB & 0b1111;

			int weaponSwitchMode = switchModes & 0b1111;
			int itemSwitchMode = (switchModes >> 4) & 0b1111;

			int voiceChatRecognitionLevel = (voiceChatA >> 4) & 0b1111;

			int voiceChatVolume = voiceChatB & 0b1111;
			int headsetVolume = (voiceChatB >> 4) & 0b1111;

			JsonObject data = new JsonObject();
			data.addProperty("onlineStatusMode", onlineStatusMode);
			data.addProperty("emailFriendsOnly", emailFriendsOnly);
			data.addProperty("receiveNotices", receiveNotices);
			data.addProperty("receiveInvites", receiveInvites);

			data.addProperty("normalViewVerticalInvert", normalViewVerticalInvert);
			data.addProperty("normalViewHorizontalInvert", normalViewHorizontalInvert);
			data.addProperty("normalViewSpeed", normalViewSpeed);
			data.addProperty("shoulderViewVerticalInvert", shoulderViewVerticalInvert);
			data.addProperty("shoulderViewHorizontalInvert", shoulderViewHorizontalInvert);
			data.addProperty("shoulderViewSpeed", shoulderViewSpeed);
			data.addProperty("firstViewVerticalInvert", firstViewVerticalInvert);
			data.addProperty("firstViewHorizontalInvert", firstViewHorizontalInvert);
			data.addProperty("firstViewSpeed", firstViewSpeed);

			data.addProperty("firstViewPlayerDirection", firstViewPlayerDirection);
			data.addProperty("viewChangeSpeed", viewChangeSpeed);
			data.addProperty("firstViewMemory", firstViewMemory);
			data.addProperty("radarLockNorth", radarLockNorth);
			data.addProperty("radarFloorHide", radarFloorHide);
			data.addProperty("hudDisplaySize", hudDisplaySize);
			data.addProperty("hudHideNameTags", hudHideNameTags);
			data.addProperty("lockOnEnabled", lockOnEnabled);

			data.addProperty("weaponSwitchMode", weaponSwitchMode);
			data.addProperty("weaponSwitchA", weaponSwitchA);
			data.addProperty("weaponSwitchB", weaponSwitchB);
			data.addProperty("weaponSwitchC", weaponSwitchC);

			data.addProperty("itemSwitchMode", itemSwitchMode);

			data.addProperty("codec1Name", codec1Name);
			data.addProperty("codec1a", codec1a);
			data.addProperty("codec1b", codec1b);
			data.addProperty("codec1c", codec1c);
			data.addProperty("codec1d", codec1d);

			data.addProperty("codec2Name", codec2Name);
			data.addProperty("codec2a", codec2a);
			data.addProperty("codec2b", codec2b);
			data.addProperty("codec2c", codec2c);
			data.addProperty("codec2d", codec2d);

			data.addProperty("codec3Name", codec3Name);
			data.addProperty("codec3a", codec3a);
			data.addProperty("codec3b", codec3b);
			data.addProperty("codec3c", codec3c);
			data.addProperty("codec3d", codec3d);

			data.addProperty("codec4Name", codec4Name);
			data.addProperty("codec4a", codec4a);
			data.addProperty("codec4b", codec4b);
			data.addProperty("codec4c", codec4c);
			data.addProperty("codec4d", codec4d);

			data.addProperty("voiceChatRecognitionLevel", voiceChatRecognitionLevel);
			data.addProperty("voiceChatVolume", voiceChatVolume);
			data.addProperty("headsetVolume", headsetVolume);
			data.addProperty("bgmVolume", bgmVolume);

			String json = Util.jsonEncode(data);

			session = DB.getSession();
			session.beginTransaction();

			character.setGameplayOptions(json);
			session.update(character);

			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception while updating gameplay options.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
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

			Character character = user.getCurrentCharacter();
			
			List<CharacterChatMacro> macros = character.getChatMacros();
			if (macros.size() <= 0) {
				for (int typem = 0; typem < 2; typem++) {
					for (int index = 0; index < 12; index++) {
						CharacterChatMacro macro = new CharacterChatMacro();
						macro.setCharacterId(character.getId());
						macro.setCharacter(character);
						macro.setType(typem);
						macro.setIndex(index);
						macros.add(macro);
					}
				}
			}
			
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
				Packets.writeError(ctx, 0x4111, 2);
				return;
			}

			ByteBuf bi = in.getPayload();
			final int type = bi.readByte();

			Character character = user.getCurrentCharacter();

			List<CharacterChatMacro> macros = character.getChatMacros();
			for (int i = 0; i < 12; i++) {
				final int index = i;
				CharacterChatMacro macro = macros.stream().filter((e) -> e.getIndex() == index && e.getType() == type)
						.findAny().orElse(null);
				String text = Util.readString(bi, 64);
				macro.setText(text);
			}

			session = DB.getSession();
			session.beginTransaction();

			for (CharacterChatMacro macro : macros) {
				session.saveOrUpdate(macro);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			if (type == 1) {
				Packets.write(ctx, 0x4111, 0);
			}
		} catch (Exception e) {
			logger.error("Exception while updating chat macros.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x4111, 1);
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

			List<CharacterEquippedSkills> skillsList = character.getSkills();
			if (skillsList.size() <= 0) {
				CharacterEquippedSkills skills = new CharacterEquippedSkills();
				skills.setCharacter(character);
				skillsList.add(skills);
			}
			CharacterEquippedSkills skills = skillsList.get(0);

			int clanId = 0;
			String clanName = "";
			int time1 = (int) Instant.now().getEpochSecond();
			int rwd = character.getId();

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
			bo.writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeInt(skillExp).writeZero(5);

			bo.writeInt(rwd);
			if (character.getComment() != null) {
				Util.writeString(character.getComment(), 128, bo);
			} else {
				bo.writeZero(128);
			}
			bo.writeInt(0).writeByte(character.getRank()).writeBytes(bytes3);

			Packets.write(ctx, 0x4122, bo);
		} catch (Exception e) {
			logger.error("Exception while getting personal info.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4122, 1);
		}
	}

	/**
	 * TODO: Update
	 * 
	 * @param ctx
	 * @param in
	 */
	public static void updatePersonalInfo(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating personal info: No User.");
				Packets.writeError(ctx, 0x4131, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			CharacterAppearance appearance = character.getAppearance().get(0);

			List<CharacterEquippedSkills> skillsList = character.getSkills();
			if (skillsList.size() <= 0) {
				CharacterEquippedSkills skills = new CharacterEquippedSkills();
				skills.setCharacter(character);
				skillsList.add(skills);
			}
			CharacterEquippedSkills skills = skillsList.get(0);

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
			int level1 = bi.readByte();
			int level2 = bi.readByte();
			int level3 = bi.readByte();
			int level4 = bi.readByte();

			bi.skipBytes(2);
			String comment = Util.readString(bi, 128);

			character.setComment(comment);

			appearance.setUpper(upper);
			appearance.setLower(lower);
			appearance.setHead(head);
			appearance.setChest(chest);
			appearance.setHands(hands);
			appearance.setWaist(waist);
			appearance.setFeet(feet);
			appearance.setAccessory1(accessory1);
			appearance.setAccessory2(accessory2);
			appearance.setUpperColor(upperColor);
			appearance.setLowerColor(lowerColor);
			appearance.setHeadColor(headColor);
			appearance.setChestColor(chestColor);
			appearance.setHandsColor(handsColor);
			appearance.setWaistColor(waistColor);
			appearance.setFeetColor(feetColor);
			appearance.setAccessory1Color(accessory1Color);
			appearance.setAccessory2Color(accessory2Color);

			skills.setSkill1(skill1);
			skills.setSkill2(skill2);
			skills.setSkill3(skill3);
			skills.setSkill4(skill4);
			skills.setLevel1(level1);
			skills.setLevel2(level2);
			skills.setLevel3(level3);
			skills.setLevel4(level4);

			session = DB.getSession();
			session.beginTransaction();

			session.update(character);
			session.update(appearance);
			session.saveOrUpdate(skills);

			session.getTransaction().commit();
			DB.closeSession(session);

			bo = ctx.alloc().directBuffer(0xba);

			bo.writeInt(0);
			bo.writeByte(upper).writeByte(lower).writeByte(facePaint).writeByte(upperColor).writeByte(lowerColor)
					.writeByte(head).writeByte(chest).writeByte(hands).writeByte(waist).writeByte(feet)
					.writeByte(accessory1).writeByte(accessory2).writeByte(headColor).writeByte(chestColor)
					.writeByte(handsColor).writeByte(waistColor).writeByte(feetColor).writeByte(accessory1Color)
					.writeByte(accessory2Color).writeByte(skill1).writeByte(skill2).writeByte(skill3).writeByte(skill4)
					.writeZero(1).writeByte(level1).writeByte(level2).writeByte(level3).writeByte(level4).writeZero(1);

			for (int i = 0; i < 4; i++) {
				int skillExp = 0x600000;
				bo.writeInt(skillExp);
			}
			bo.writeZero(5);

			Util.writeString(comment, 128, bo);

			Packets.write(ctx, 0x4131, bo);
		} catch (Exception e) {
			logger.error("Exception while updating personal info.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4131, 1);
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
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting gear: No User.");
				Packets.writeError(ctx, 0x4124, 2);
				return;
			}

			int[] gearItems = new int[] { 0x04, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x16, 0x1C, 0x1D,
					0x1E, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2E, 0x2F,
					0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40, 0x44, 0x45,
					0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x56, 0x57,
					0x58, 0x59, 0x5A, 0x5B, 0x5C, 0x5D, 0x5E, 0x5F, 0x60, 0x61, 0x62, 0x63, 0x64, 0x66, 0x67, 0x68,
					0x69, 0x6A, 0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x80,
					0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C, 0x8D, 0x8E, 0x8F,
					0xA0, 0xA1, 0xA2, 0xB0, 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xF0, 0xF1, 0xF2, 0xF3, 0xF4 };

			bo = ctx.alloc().directBuffer(0x24 + gearItems.length * 0x5);

			bo.writeInt(gearItems.length);

			for (int gearItem : gearItems) {
				int colors = 0xffffffff;
				bo.writeByte(gearItem).writeInt(colors);
			}

			bo.writeBytes(GEAR_TERMINATOR);

			Packets.write(ctx, 0x4124, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gear.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4124, 1);
		}
	}

	public static void getSkills(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting skills: No User.");
				Packets.writeError(ctx, 0x4125, 2);
				return;
			}

			int numSkills = 25;

			bo = ctx.alloc().directBuffer(0x4 + numSkills * 0x4);

			bo.writeInt(numSkills);

			for (int i = 1; i <= numSkills; i++) {
				int exp;
				if (i == 17 || i == 20 || i == 22) {
					exp = 0x2000;
				} else {
					exp = 0x6000;
				}
				bo.writeByte(i).writeShort(exp).writeZero(1);
			}

			Packets.write(ctx, 0x4125, bo);
		} catch (Exception e) {
			logger.error("Exception while getting skills.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4125, 1);
		}
	}

	public static void getSkillSets(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting skill sets: No User.");
				Packets.writeError(ctx, 0x4140, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			List<CharacterSetSkills> sets = character.getSetsSkills();
			if (sets == null) {
				sets = new ArrayList<CharacterSetSkills>();
				for (int index = 0; index < 3; index++) {
					CharacterSetSkills set = new CharacterSetSkills();
					set.setCharacter(character);
					set.setIndex(index);
				}
				character.setSetsSkills(sets);
			}

			bo = ctx.alloc().directBuffer(0x4d * sets.size());

			for (CharacterSetSkills set : sets) {
				bo.writeInt(set.getModes()).writeByte(set.getSkill1()).writeByte(set.getSkill2())
						.writeByte(set.getSkill3()).writeByte(set.getSkill4()).writeZero(1).writeByte(set.getLevel1())
						.writeByte(set.getLevel2()).writeByte(set.getLevel3()).writeByte(set.getLevel4()).writeZero(1);
				Util.writeString(set.getName(), 63, bo, StandardCharsets.UTF_8);
			}

			Packets.write(ctx, 0x4140, bo);
		} catch (Exception e) {
			logger.error("Exception while getting skill sets.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4140, 1);
		}
	}

	public static void updateSkillSets(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating skill sets: No User.");
				Packets.writeError(ctx, 0x4141, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			List<CharacterSetSkills> sets = character.getSetsSkills();
			if (sets == null) {
				sets = new ArrayList<CharacterSetSkills>();
				for (int index = 0; index < 3; index++) {
					CharacterSetSkills set = new CharacterSetSkills();
					set.setCharacter(character);
					set.setIndex(index);
				}
				character.setSetsSkills(sets);
			}

			ByteBuf bi = in.getPayload();

			for (int i = 0; i < 3; i++) {
				CharacterSetSkills set = sets.get(i);

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

				set.setName(name);
				set.setModes(modes);
				set.setSkill1(skill1);
				set.setSkill2(skill2);
				set.setSkill3(skill3);
				set.setSkill4(skill4);
				set.setLevel1(level1);
				set.setLevel2(level2);
				set.setLevel3(level3);
				set.setLevel4(level4);
			}

			session = DB.getSession();
			session.beginTransaction();

			for (int i = 0; i < 3; i++) {
				session.saveOrUpdate(sets.get(i));
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4141, 0);
		} catch (Exception e) {
			logger.error("Exception while updating skill sets.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x4141, 1);
		}
	}

	public static void getGearSets(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting skill sets: No User.");
				Packets.writeError(ctx, 0x4142, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			List<CharacterSetGear> sets = character.getSetsGear();
			if (sets.size() <= 0) {
				for (int index = 0; index < 3; index++) {
					CharacterSetGear set = new CharacterSetGear();
					set.setCharacterId(character.getId());
					set.setCharacter(character);
					set.setIndex(index);
					set.setFace(0);
					set.setHead(0);
					set.setHeadColor(0);
					set.setUpper(0);
					set.setUpperColor(0);
					set.setLower(0);
					set.setLowerColor(0);
					set.setChest(0);
					set.setChestColor(0);
					set.setWaist(0);
					set.setWaistColor(0);
					set.setHands(0);
					set.setHandsColor(0);
					set.setFeet(0);
					set.setFeetColor(0);
					set.setAccessory1(0);
					set.setAccessory1Color(0);
					set.setAccessory2(0);
					set.setAccessory2Color(0);
					set.setFacePaint(0);
					sets.add(set);
				}
			}

			bo = ctx.alloc().directBuffer(0x57 * sets.size());

			for (CharacterSetGear set : sets) {
				bo.writeInt(set.getStages()).writeByte(set.getFace()).writeByte(set.getHead()).writeByte(set.getUpper())
						.writeByte(set.getLower()).writeByte(set.getChest()).writeByte(set.getWaist())
						.writeByte(set.getHands()).writeByte(set.getFeet()).writeByte(set.getAccessory1())
						.writeByte(set.getAccessory2()).writeByte(set.getHeadColor()).writeByte(set.getUpperColor())
						.writeByte(set.getLowerColor()).writeByte(set.getChestColor()).writeByte(set.getWaistColor())
						.writeByte(set.getHandsColor()).writeByte(set.getFeetColor())
						.writeByte(set.getAccessory1Color()).writeByte(set.getAccessory2Color())
						.writeByte(set.getFacePaint());
				Util.writeString(set.getName(), 63, bo, StandardCharsets.UTF_8);
			}

			Packets.write(ctx, 0x4142, bo);
		} catch (Exception e) {
			logger.error("Exception while getting gear presets.", e);
			Packets.writeError(ctx, 0x4142, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updateGearSets(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating gear sets: No User.");
				Packets.writeError(ctx, 0x4143, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			
			List<CharacterSetGear> sets = character.getSetsGear();

			ByteBuf bi = in.getPayload();

			for (int i = 0; i < 3; i++) {
				CharacterSetGear set = sets.get(0);
				
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

				set.setName(name);
				set.setStages(stages);
				set.setFace(face);
				set.setHead(head);
				set.setUpper(upper);
				set.setLower(lower);
				set.setChest(chest);
				set.setWaist(waist);
				set.setHands(hands);
				set.setFeet(feet);
				set.setAccessory1(accessory1);
				set.setAccessory2(accessory2);
				set.setHeadColor(headColor);
				set.setUpperColor(upperColor);
				set.setLowerColor(lowerColor);
				set.setChestColor(chestColor);
				set.setWaistColor(waistColor);
				set.setHandsColor(handsColor);
				set.setFeetColor(feetColor);
				set.setAccessory1Color(accessory1Color);
				set.setAccessory2Color(accessory2Color);
				set.setFacePaint(facePaint);				
			}

			session = DB.getSession();
			session.beginTransaction();
			
			for (CharacterSetGear set : sets) {
				session.saveOrUpdate(set);
			}
			
			session.getTransaction().commit();
			DB.closeSession(session);
			
			Packets.write(ctx, 0x4143, 0);
		} catch (Exception e) {
			logger.error("Exception while updating gear sets.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x4143, 1);
		}
	}

	public static void updateConnectionInfo(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating connection info: No User.");
				Packets.writeError(ctx, 0x4701, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			List<ConnectionInfo> connectionInfos = character.getConnectionInfo();
			if (connectionInfos.size() <= 0) {
				ConnectionInfo info = new ConnectionInfo();
				info.setCharacter(character);
				connectionInfos.add(info);
			}
			ConnectionInfo info = connectionInfos.get(0);

			ByteBuf bi = in.getPayload();
			int privatePort = bi.readUnsignedShort();
			String privateIp = Util.readString(bi, 16);
			int publicPort = bi.readUnsignedShort();
			@SuppressWarnings("unused")
			short unk = bi.readShort();

			String publicIp = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();

			info.setPublicIp(publicIp);
			info.setPublicPort(publicPort);
			info.setPrivateIp(privateIp);
			info.setPrivatePort(privatePort);

			session = DB.getSession();
			session.beginTransaction();

			session.saveOrUpdate(info);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4701, 0);
		} catch (Exception e) {
			logger.error("Exception while updating connection info.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x4701, 1);
		}
	}

	public static void getMail(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4821, 0);
		Packets.write(ctx, 0x4823, 0);
	}

	public static void getFriendsBlockedList(ChannelHandlerContext ctx, Packet in) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting friends/blocked list: No User.");
				Packets.writeError(ctx, 0x4581, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			ByteBuf bi = in.getPayload();
			int type = bi.readByte();

			if (type == 0) {
				List<CharacterFriend> friends = character.getFriends();

				session = DB.getSession();
				session.beginTransaction();

				for (CharacterFriend friend : friends) {
					session.update(friend);
					Hibernate.initialize(friend.getTarget());
					Hibernate.initialize(friend.getTarget().getLobby());
				}

				session.getTransaction().commit();
				DB.closeSession(session);

				Packets.handleMutliElementPayload(ctx, friends.size(), 23, 0x2b, payloads, (i, bo) -> {
					CharacterFriend friend = friends.get(i);

					int gameId = 0;
					String gameHostName = "";
					int gameType = 0;

					bo.writeInt(friend.getTargetId());
					Util.writeString(friend.getTarget().getName(), 16, bo);
					bo.writeShort(friend.getTarget().getLobbyId()).writeInt(gameId);
					Util.writeString(gameHostName, 16, bo);
					bo.writeByte(gameType);
				});
			} else {
				List<CharacterBlocked> blocked = character.getBlocked();

				session = DB.getSession();
				session.beginTransaction();

				for (CharacterBlocked block : blocked) {
					session.update(block);
					Hibernate.initialize(block.getTarget());
					Hibernate.initialize(block.getTarget().getLobby());
				}

				session.getTransaction().commit();
				DB.closeSession(session);

				Packets.handleMutliElementPayload(ctx, blocked.size(), 23, 0x2b, payloads, (i, bo) -> {
					CharacterBlocked block = blocked.get(i);

					int gameId = 0;
					String gameHostName = "";
					int gameType = 0;

					bo.writeInt(block.getTargetId());
					Util.writeString(block.getTarget().getName(), 16, bo);
					bo.writeShort(block.getTarget().getLobbyId()).writeInt(gameId);
					Util.writeString(gameHostName, 16, bo);
					bo.writeByte(gameType);
				});
			}

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
			Packets.writeError(ctx, 0x4103, 0xff);
			// ByteBuf bo1 = Util.readFile(new File("personal-stats-1.bin"));
			// ByteBuf bo2 = Util.readFile(new File("personal-stats-2.bin"));
			// ByteBuf bo3 = Util.readFile(new File("personal-stats-3.bin"));
			// Packets.write(ctx, 0x4103, bo1);
			// Packets.write(ctx, 0x4105, bo2);
			// Packets.write(ctx, 0x4107, bo3);
		} catch (Exception e) {
			logger.error("Exception while getting personal stats.", e);
			Packets.writeError(ctx, 0x4103, 1);
		}
	}

	/**
	 * TODO: Check against 64-player limit
	 * 
	 * @param ctx
	 * @param in
	 */
	public static void addFriendsBlocked(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while adding to friends/blocked list: No User.");
				Packets.writeError(ctx, 0x4502, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			ByteBuf bi = in.getPayload();
			int type = bi.readByte();
			int targetId = bi.readInt();

			session = DB.getSession();
			session.beginTransaction();

			Character target = session.get(Character.class, targetId);

			if (type == 0) {
				List<CharacterFriend> friends = character.getFriends();

				CharacterFriend friend = new CharacterFriend();
				friend.setCharacterId(character.getId());
				friend.setCharacter(character);
				friend.setTargetId(target.getId());
				friend.setTarget(target);

				session.saveOrUpdate(friend);
				friends.add(friend);
			} else {
				List<CharacterBlocked> blocked = character.getBlocked();

				CharacterBlocked block = new CharacterBlocked();
				block.setCharacterId(character.getId());
				block.setCharacter(character);
				block.setTargetId(target.getId());
				block.setTarget(target);

				session.saveOrUpdate(block);
				session.refresh(block);
				blocked.add(block);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			bo = ctx.alloc().directBuffer(0x19);
			bo.writeInt(0).writeInt(target.getId()).writeByte(type);
			Util.writeString(target.getName(), 16, bo);

			Packets.write(ctx, 0x4502, bo);
		} catch (Exception e) {
			logger.error("Exception while adding to friends/blocked list.", e);
			Util.safeRelease(bo);
			Packets.writeError(ctx, 0x4502, 1);
		}
	}

	public static void removeFriendsBlocked(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while removing from friends/blocked list: No User.");
				Packets.writeError(ctx, 0x4512, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			ByteBuf bi = in.getPayload();
			int type = bi.readByte();
			int targetId = bi.readInt();

			session = DB.getSession();
			session.beginTransaction();

			boolean removed = false;
			if (type == 0) {
				List<CharacterFriend> friends = character.getFriends();
				CharacterFriend friendToRemove = null;
				for (CharacterFriend friend : friends) {
					if (friend.getTargetId() == targetId) {
						friendToRemove = friend;
						break;
					}
				}

				if (friendToRemove != null) {
					friends.remove(friendToRemove);
					session.remove(friendToRemove);
					removed = true;
				}
			} else {
				List<CharacterBlocked> blocked = character.getBlocked();
				CharacterBlocked blockToRemove = null;
				for (CharacterBlocked block : blocked) {
					if (block.getTargetId() == targetId) {
						blockToRemove = block;
						break;
					}
				}

				if (blockToRemove != null) {
					blocked.remove(blockToRemove);
					session.remove(blockToRemove);
					removed = true;
				}
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			if (!removed) {
				logger.error("Error while removing from friends/blocked list: No entry.");
				Packets.writeError(ctx, 0x4512, 3);
				return;
			}

			bo = ctx.alloc().directBuffer(0x9);
			bo.writeInt(0).writeByte(type).writeInt(targetId);

			Packets.write(ctx, 0x4512, bo);
		} catch (Exception e) {
			logger.error("Exception while removing from friends/blocked list.", e);
			Packets.writeError(ctx, 0x4512, 1);
			Util.safeRelease(bo);
		}
	}

	public static void search(ChannelHandlerContext ctx, Packet in) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		Session session = null;
		try {
			ByteBuf bi = in.getPayload();
			boolean exactOnly = bi.readBoolean();
			boolean caseSensitive = bi.readBoolean();
			String name = Util.readString(bi, 0x10);

			session = DB.getSession();
			session.beginTransaction();

			if (!exactOnly) {
				name = "%" + name + "%";
			}

			Query<Character> query = session.createQuery("from Character where name like :name", Character.class);
			query.setParameter("name", name);
			List<Character> characters = query.list();

			Hibernate.initialize(characters);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.handleMutliElementPayload(ctx, characters.size(), 17, 0x3b, payloads, (i, bo) -> {
				Character character = characters.get(i);

				int gameId = 0;
				String gameHostName = "";
				int gameType = 0;

				bo.writeInt(character.getId());
				Util.writeString(character.getName(), 16, bo);
				bo.writeShort(character.getLobbyId());
				Util.writeString(character.getLobby().getName(), 16, bo);
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

	public static void getCharacterCard(ChannelHandlerContext ctx, Packet in) {
		// In: 00000002 - Player ID
		try {
			ByteBuf bo1 = Util.readFile(new File("player-overview.bin"));
			Packets.write(ctx, 0x4221, bo1);
		} catch (Exception e) {
			logger.error("Exception while getting character card.", e);
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
