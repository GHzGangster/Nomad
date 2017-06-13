package savemgo.nomad.helper;

import java.nio.charset.StandardCharsets;
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
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NChannels;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.lobby.GameLobby;
import savemgo.nomad.packet.Packet;
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
				Packets.write(ctx, 0x3004, 1);
				return false;
			}

			boolean okay = isCharaId ? id == user.getCurrentCharacterId() : id == user.getId();
			if (!okay) {
				logger.error("Error while checking session: Bad id.");
				Packets.write(ctx, 0x3004, 1);
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
				Packets.write(ctx, 0x3004, 2);
				return false;
			}

			Packets.write(ctx, 0x3004, 0);
		} catch (Exception e) {
			logger.error("Exception while checking session.", e);
			DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x3004, 1);
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
				Packets.writeError(ctx, 0x3049, 2);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> query = session
					.createQuery("from Character as c inner join fetch c.appearance where user=:user", Character.class);
			query.setParameter("user", user);
			List<Character> characters = query.list();

			session.getTransaction().commit();
			DB.closeSession(session);

			int numCharacters = characters.size();

			bo = ctx.alloc().directBuffer(0x1d7);
			bo.writeInt(0).writeByte(user.getSlots()).writeByte(numCharacters).writeZero(1);

			for (int i = 0; i < numCharacters; i++) {
				Character chara = characters.get(i);
				CharacterAppearance app = chara.getAppearance().get(0);

				if (i == 0) {
					Util.writeString(chara.getName(), 16, bo);
					bo.writeZero(1);
				} else {
					bo.writeInt(i);
				}

				bo.writeInt(chara.getId());
				Util.writeString(chara.getName(), 16, bo);
				bo.writeByte(app.getGender()).writeByte(app.getFace()).writeByte(app.getUpper())
						.writeByte(app.getLower()).writeByte(app.getFacePaint()).writeByte(app.getUpperColor())
						.writeByte(app.getLowerColor()).writeByte(app.getVoice()).writeByte(app.getPitch()).writeZero(4)
						.writeByte(app.getHead()).writeByte(app.getChest()).writeByte(app.getHands())
						.writeByte(app.getWaist()).writeByte(app.getFeet()).writeByte(app.getAccessory1())
						.writeByte(app.getAccessory2()).writeByte(app.getHeadColor()).writeByte(app.getChestColor())
						.writeByte(app.getHandsColor()).writeByte(app.getWaistColor()).writeByte(app.getFeetColor())
						.writeByte(app.getAccessory1Color()).writeByte(app.getAccessory2Color()).writeZero(1);
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
			Packets.writeError(ctx, 0x3049, 1);
		}
	}

	public static void createCharacter(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting character list: No User.");
				Packets.writeError(ctx, 0x3102, 2);
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

			Character character = new Character();
			character.setName(name);
			character.setUser(user);

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
			Packets.writeError(ctx, 0x3102, 1);
		}
	}

	public static void selectCharacter(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while selecting character: No User.");
				Packets.writeError(ctx, 0x3104, 2);
				return;
			}

			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> query = session.createQuery("from Character where user=:user", Character.class);
			query.setParameter("user", user);
			List<Character> characters = query.list();

			int numCharacters = characters.size();
			if (index < 0 || index > numCharacters - 1) {
				index = 0;
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
			Packets.writeError(ctx, 0x3104, 1);
		}
	}

	public static void deleteCharacter(ChannelHandlerContext ctx, Packet in) {
		// Session session = null;
		try {
			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			// session = DB.getSession();
			// session.beginTransaction();
			//
			// session.getTransaction().commit();
			// DB.closeSession(session);

			Packets.write(ctx, 0x3106, 0);
		} catch (Exception e) {
			logger.error("Exception while deleting character.", e);
			// DB.rollbackAndClose(session);
			Packets.writeError(ctx, 0x3106, 1);
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
				Hibernate.initialize(character.getAppearance());
				Hibernate.initialize(character.getBlocked());
				Hibernate.initialize(character.getChatMacros());
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
			if (user == null) {
				logger.error("Error while disconnecting from lobby: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			if (character != null) {
				Player player = Hibernate.isInitialized(character.getPlayer()) && character.getPlayer() != null
						&& character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
				if (player != null) {
					Game game = Hibernate.isInitialized(player.getGame()) ? player.getGame() : null;
					if (game != null && character.getId() == game.getHost().getId()) {
						// Hosting a game, but disconnected
						Hosts.quitGame(ctx);
					}
				}

				character.setLobby(null);

				session = DB.getSession();
				session.beginTransaction();

				session.update(character);

				session.getTransaction().commit();
				DB.closeSession(session);
			}

			NUsers.remove(ctx.channel());
		} catch (Exception e) {
			logger.error("Exception while disconnecting from lobby.", e);
			DB.rollbackAndClose(session);
		}
	}

}
