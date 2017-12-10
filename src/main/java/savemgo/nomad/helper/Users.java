package savemgo.nomad.helper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.crypto.Crypto;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.entity.MessageClanApplication;
import savemgo.nomad.entity.ClanMember;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NChannels;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Users {

	private static final Logger logger = LogManager.getLogger(Users.class);

	private static final byte[] XOR_SESSION_ID = new byte[] { (byte) 0x35, (byte) 0xd5, (byte) 0xc3, (byte) 0x8e,
			(byte) 0xd0, (byte) 0x11, (byte) 0x0e, (byte) 0xa8 };

	/**
	 * Definitely <i>not</i> the right way to do this...<br>
	 * Should figure out the right way in the future...
	 * 
	 * @param full
	 * @return
	 */
	public static byte[] decryptSessionId(byte[] full) {
		byte[] bytes = new byte[8];
		System.arraycopy(full, 0, bytes, 0, bytes.length);
		Util.xor(bytes, XOR_SESSION_ID);
		return Crypto.instanceAuth().encrypt(bytes);
	}

	private static final byte SPECIAL_SESSION_BYTES[] = { (byte) 0xE7, (byte) 0xBA, (byte) 0xB4, (byte) 0x26,
			(byte) 0xFE, (byte) 0x3F, (byte) 0x40, (byte) 0x73, (byte) 0xDB, (byte) 0x94, (byte) 0x36, (byte) 0xDF,
			(byte) 0x6D, (byte) 0xDB, (byte) 0xD3, (byte) 0x9C };

	public static boolean checkSession(ChannelHandlerContext ctx, Packet in, Lobby lobby, boolean isCharaId) {
		Session session = null;
		try {
			ByteBuf bi = in.getPayload();
			int id = bi.readInt();
			byte[] bytes = new byte[16];
			bi.readBytes(bytes);

			byte[] mgo2SessionBytes = decryptSessionId(bytes);
			String mgo2Session = new String(mgo2SessionBytes, StandardCharsets.ISO_8859_1);

			if (Arrays.equals(bytes, SPECIAL_SESSION_BYTES)) {
				mgo2Session = "cafebabe";
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<User> query = session.createQuery("from User where session=:session", User.class);
			query.setParameter("session", mgo2Session);

			User user = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (user == null) {
				logger.error("Error while checking session: Bad session.");
				Packets.write(ctx, 0x3004, Error.INVALID_SESSION);
				return false;
			}

			boolean okay = isCharaId ? id == user.getCurrentCharacterId() : id == user.getId();
			if (!okay) {
				logger.error("Error while checking session: Bad id ... {} {} -- {} {}", isCharaId, id, user.getCurrentCharacterId(), user.getId());
				Packets.write(ctx, 0x3004, Error.INVALID_SESSION);
				return false;
			}

			if (!isCharaId) {
				session = DB.getSession();
				session.beginTransaction();

				user.setCurrentCharacter(null);
				session.update(user);

				session.getTransaction().commit();
				DB.closeSession(session);
			}

			if (!onLobbyConnected(ctx, lobby, user)) {
				Packets.write(ctx, 0x3004, Error.INVALID_SESSION);
				return false;
			}

			Packets.write(ctx, 0x3004, 0);
		} catch (Exception e) {
			logger.error("Exception while checking session.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x3004, Error.GENERAL);
			return false;
		}
		return true;
	}

	public static void getCharacterList(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting character list: No User.");
				Packets.write(ctx, 0x3049, Error.INVALID_SESSION);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> query = session.createQuery(
					"from Character as c inner join fetch c.appearance where user=:user and c.active=1",
					Character.class);
			query.setParameter("user", user);
			List<Character> characters = query.list();

			session.getTransaction().commit();
			DB.closeSession(session);

			int numCharacters = characters.size();

			if (user.getMainCharacterId() != null) {
				for (int i = 0; i < numCharacters; i++) {
					Character character = characters.get(i);
					if (character.getId().equals(user.getMainCharacterId())) {
						characters.remove(i);
						characters.add(0, character);
					}
				}
			}
			
			bo = ctx.alloc().directBuffer(0x1d7);
			bo.writeInt(0).writeByte(user.getSlots()).writeByte(numCharacters).writeZero(1);

			// Make MAIN show up first!
			
			for (int i = 0; i < numCharacters; i++) {
				Character character = characters.get(i);
				CharacterAppearance appearance = Util.getFirstOrNull(character.getAppearance());

				String name = null;
				if (user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId())) {
					name = "*" + character.getName();
				} else {
					name = character.getName();
				}

				if (i == 0) {
					Util.writeString(name, 16, bo);
					bo.writeZero(1);
				} else {
					bo.writeInt(i);
				}

				bo.writeInt(character.getId());
				Util.writeString(name, 16, bo);
				bo.writeByte(appearance.getGender()).writeByte(appearance.getFace()).writeByte(appearance.getUpper())
						.writeByte(appearance.getLower()).writeByte(appearance.getFacePaint())
						.writeByte(appearance.getUpperColor()).writeByte(appearance.getLowerColor())
						.writeByte(appearance.getVoice()).writeByte(appearance.getPitch()).writeZero(4)
						.writeByte(appearance.getHead()).writeByte(appearance.getChest())
						.writeByte(appearance.getHands()).writeByte(appearance.getWaist())
						.writeByte(appearance.getFeet()).writeByte(appearance.getAccessory1())
						.writeByte(appearance.getAccessory2()).writeByte(appearance.getHeadColor())
						.writeByte(appearance.getChestColor()).writeByte(appearance.getHandsColor())
						.writeByte(appearance.getWaistColor()).writeByte(appearance.getFeetColor())
						.writeByte(appearance.getAccessory1Color()).writeByte(appearance.getAccessory2Color())
						.writeZero(1);
			}

			byte[] bytes1 = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00, (byte) 0x03,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
					(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };

			bo.writeZero(0x1b4 - bo.writerIndex());
			bo.writeBytes(bytes1);

			Packets.write(ctx, 0x3049, bo);
		} catch (Exception e) {
			logger.error("Exception while getting character list.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x3049, Error.GENERAL);
		}
	}

	public static void createCharacter(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting character list: No User.");
				Packets.write(ctx, 0x3102, Error.INVALID_SESSION);
				return;
			}

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

			if (name.startsWith(":#") || name.startsWith("GM_") || name.startsWith("GM-") || name.startsWith("GM.")
					|| name.startsWith("GM,")) {
				logger.error("Error while creating character: Reserved prefix.");
				Packets.write(ctx, 0x3102, Error.CHAR_NAMEPREFIX);
				return;
			} else if (name.equalsIgnoreCase("SaveMGO")) {
				logger.error("Error while creating character: Reserved name.");
				Packets.write(ctx, 0x3102, Error.CHAR_NAMERESERVED);
				return;
			} else if (!Util.checkName(name)) {
				logger.error("Error while creating character: Invalid name.");
				Packets.write(ctx, 0x3102, Error.CHAR_NAMEINVALID);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> queryC = session.createQuery("from Character c where c.name = :name", Character.class);
			queryC.setParameter("name", name);
			Character characterWithName = queryC.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (characterWithName != null) {
				logger.error("Error while creating character: Name is taken.");
				Packets.write(ctx, 0x3102, Error.CHAR_NAMETAKEN);
				return;
			}

			long time = Instant.now().getEpochSecond();

			Character character = new Character();
			character.setName(name);
			character.setUser(user);
			character.setCreationTime((int) time);
			character.setActive(1);

			CharacterAppearance appearance = new CharacterAppearance();
			character.setAppearance(Arrays.asList(appearance));
			appearance.setCharacter(character);
			appearance.setGender(gender);
			appearance.setFace(face);
			appearance.setVoice(voice);
			appearance.setPitch(pitch);
			appearance.setHead(head);
			appearance.setHeadColor(headColor);
			appearance.setUpper(upper);
			appearance.setUpperColor(upperColor);
			appearance.setLower(lower);
			appearance.setLowerColor(lowerColor);
			appearance.setChest(chest);
			appearance.setChestColor(chestColor);
			appearance.setWaist(waist);
			appearance.setWaistColor(waistColor);
			appearance.setHands(hands);
			appearance.setHandsColor(handsColor);
			appearance.setFeet(feet);
			appearance.setFeetColor(feetColor);
			appearance.setAccessory1(accessory1);
			appearance.setAccessory1Color(accessory1Color);
			appearance.setAccessory2(accessory2);
			appearance.setAccessory2Color(accessory2Color);
			appearance.setFacePaint(facePaint);

			user.setCurrentCharacter(character);
			if (user.getMainCharacterId() == null) {
				user.setMainCharacter(character);
			}

			session = DB.getSession();
			session.beginTransaction();

			session.save(character);
			session.save(appearance);
			session.update(user);

			session.getTransaction().commit();
			DB.closeSession(session);

			bo = ctx.alloc().directBuffer(8);
			bo.writeInt(0).writeInt(character.getId());

			Packets.write(ctx, 0x3102, bo);
		} catch (Exception e) {
			logger.error("Exception while creating character.", e);
			DB.rollbackAndClose(session);
			Util.safeRelease(bo);
			Packets.write(ctx, 0x3102, Error.GENERAL);
		}
	}

	public static void selectCharacter(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while selecting character: No User.");
				Packets.write(ctx, 0x3104, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> query = session.createQuery("from Character c where user=:user and c.active=1",
					Character.class);
			query.setParameter("user", user);
			List<Character> characters = query.list();
			
			int numCharacters = characters.size();
			if (index < 0 || index > numCharacters - 1) {
				index = 0;
			}
			
			if (user.getMainCharacterId() != null) {
				for (int i = 0; i < numCharacters; i++) {
					Character character = characters.get(i);
					if (character.getId().equals(user.getMainCharacterId())) {
						characters.remove(i);
						characters.add(0, character);
					}
				}
			}

			Character character = characters.get(index);
			user.setCurrentCharacter(character);

			session.update(user);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x3104, 0);
		} catch (Exception e) {
			logger.error("Exception while selecting character.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x3104, Error.GENERAL);
		}
	}

	public static void deleteCharacter(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while deleting character: No User.");
				Packets.write(ctx, 0x3106, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> query = session.createQuery(
					"from Character as c inner join fetch c.appearance where user=:user and c.active=1",
					Character.class);
			query.setParameter("user", user);
			List<Character> characters = query.list();
			int numCharacters = characters.size();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (index < 0 || numCharacters - 1 < index) {
				index = numCharacters - 1;
			}
			
			if (user.getMainCharacterId() != null) {
				for (int i = 0; i < numCharacters; i++) {
					Character character = characters.get(i);
					if (character.getId().equals(user.getMainCharacterId())) {
						characters.remove(i);
						characters.add(0, character);
					}
				}
			}

			Character character = characters.get(index);

			boolean canDelete = true;
			if (character.getCreationTime() != null) {
				long time = Instant.now().getEpochSecond();
				long canDeleteTime = character.getCreationTime() + 7 * 24 * 60 * 60;
				if (time < canDeleteTime) {
					canDelete = false;
				}
			}

			if (!canDelete) {
				logger.error("Error while deleting character: Can't delete yet.");
				Packets.write(ctx, 0x3106, Error.CHAR_CANTDELETEYET);
				return;
			}

			character.setActive(0);
			character.setOldName(character.getName());
			character.setName(":#" + character.getId());

			session = DB.getSession();
			session.beginTransaction();

			session.update(character);

			if (user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId())) {
				User aUser = session.get(User.class, user.getId());
				aUser.setMainCharacter(null);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x3106, 0);
		} catch (Exception e) {
			logger.error("Exception while deleting character.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x3106, Error.GENERAL);
		}
	}

	public static boolean onLobbyConnected(ChannelHandlerContext ctx, Lobby lobby, User user) {
		Session session = null;
		try {
			session = DB.getSession();
			session.beginTransaction();

			session.update(user);

			if (user.getCurrentCharacterId() != null && user.getCurrentCharacter() != null) {
				Character character = user.getCurrentCharacter();
				Hibernate.initialize(character);
				character.setLobby(lobby);
				character.setLobbyId(lobby.getId());
				Hibernate.initialize(character.getAppearance());
				Hibernate.initialize(character.getBlocked());
				Hibernate.initialize(character.getChatMacros());
				Hibernate.initialize(character.getClanApplication());
				Hibernate.initialize(character.getClanMember());
				ClanMember clanMember = character.getClanMember().size() > 0 ? character.getClanMember().get(0) : null;
				if (clanMember != null) {
					Hibernate.initialize(clanMember.getClan());
				}
				Hibernate.initialize(character.getConnectionInfo());
				Hibernate.initialize(character.getFriends());
				Hibernate.initialize(character.getHostSettings());
				Hibernate.initialize(character.getPlayer());
				Hibernate.initialize(character.getSetsGear());
				Hibernate.initialize(character.getSetsSkills());
				Hibernate.initialize(character.getSkills());

				session.update(character);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			user.setChannel(ctx.channel());
			NChannels.add(ctx.channel());
			NUsers.add(ctx.channel(), user);
		} catch (Exception e) {
			logger.error("Exception while handling lobby connection.", e);
			DB.rollbackAndClose(session);
		}
		return true;
	}

	public static void onLobbyDisconnected(ChannelHandlerContext ctx, Lobby lobby) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user != null) {
				Character character = user.getCurrentCharacter();
				logger.debug("Disconnecting from lobby {}: Character - {}", user.getId(), character);
				if (user.getCurrentCharacterId() != null && character != null) {
					Player player = Hibernate.isInitialized(character.getPlayer()) && character.getPlayer() != null
							&& character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
					logger.debug("Disconnecting from lobby {}: Player - {}", user.getId(), player);
					if (player != null) {
						logger.debug("Disconnecting from lobby {}: Quitting game.", user.getId());
						Games.quitGame(ctx, false);
					}

					character.setLobby(null);

					session = DB.getSession();
					session.beginTransaction();

					session.update(character);

					session.getTransaction().commit();
					DB.closeSession(session);
				}

				NUsers.remove(ctx.channel());
			}
		} catch (Exception e) {
			logger.error("Exception while disconnecting from lobby.", e);
			DB.rollbackAndClose(session);
		}
	}

	public static void updateUserClan(ChannelHandlerContext ctx) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				return;
			}

			Character character = user.getCurrentCharacter();
			if (user.getCurrentCharacterId() == null || character == null) {
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<MessageClanApplication> queryA = session.createQuery(
					"from MessageClanApplication a join fetch a.clan where a.character = :character",
					MessageClanApplication.class);
			queryA.setParameter("character", character);

			MessageClanApplication application = queryA.uniqueResult();

			Query<ClanMember> query = session.createQuery(
					"from ClanMember m join fetch m.clan where m.character = :character", ClanMember.class);
			query.setParameter("character", character);

			ClanMember member = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			List<MessageClanApplication> applications = new ArrayList<>();
			applications.add(application);
			character.setClanApplication(applications);

			List<ClanMember> members = new ArrayList<>();
			members.add(member);
			character.setClanMember(members);
		} catch (Exception e) {
			logger.error("Exception while updating user clan.", e);
			DB.rollbackAndClose(session);
		}
	}

}
