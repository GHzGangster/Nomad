package savemgo.nomad.helper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.query.Query;

import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.crypto.Crypto;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterAppearance;
import savemgo.nomad.entity.User;
import savemgo.nomad.instance.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Accounts {

	private static final Logger logger = LogManager.getLogger(Accounts.class);

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

	public static boolean checkSession(ChannelHandlerContext ctx, Packet in, int lobbyId, boolean isCharaId) {
		Session session = null;
		try {
			ByteBuf bi = in.getPayload();
			int id = bi.readInt();
			byte[] bytes = new byte[16];
			bi.readBytes(bytes);

			byte[] mgo2SessionBytes = decryptSessionId(bytes);
			String mgo2Session = new String(mgo2SessionBytes, StandardCharsets.ISO_8859_1);

			session = DB.getSession();
			session.beginTransaction();

			Query<User> query = session.createQuery("FROM User WHERE session=:session", User.class);
			query.setParameter("session", mgo2Session);

			User user = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (user == null) {
				logger.error("Error while checking session: Bad session.");
				Packets.write(ctx, 0x3004, 1);
				return false;
			}

			boolean okay = isCharaId ? id == user.getCharacter() : id == user.getId();
			if (!okay) {
				logger.error("Error while checking session: Bad id.");
				Packets.write(ctx, 0x3004, 1);
				return false;
			}

			if (!onLobbyConnected(ctx, lobbyId, user)) {
				Packets.write(ctx, 0x3004, 2);
				return false;
			}

			Packets.write(ctx, 0x3004, 0);
		} catch (Exception e) {
			logger.error("Exception while checking session.", e);
			DB.closeSession(session);
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

			Query<Character> query = session.createQuery(
					"FROM Character as c INNER JOIN FETCH c.appearances WHERE user=:user", Character.class);
			query.setParameter("user", user.getId());
			List<Character> characters = query.list();
			
			session.getTransaction().commit();
			DB.closeSession(session);

			int numCharacters = characters.size();
			
			bo = ctx.alloc().directBuffer(0x1d7);
			bo.writeInt(0).writeByte(user.getSlots()).writeByte(numCharacters).writeZero(1);

			for (int i = 0; i < numCharacters; i++) {
				Character chara = characters.get(i);
				CharacterAppearance app = chara.getAppearances().get(0);

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
			DB.rollback(session);
			DB.closeSession(session);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x3049, 1);
		}
	}

	public static void createCharacter(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
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

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("name", name);
			data.addProperty("gender", gender);
			data.addProperty("face", face);
			data.addProperty("voice", voice);
			data.addProperty("pitch", pitch);
			data.addProperty("head", head);
			data.addProperty("headColor", headColor);
			data.addProperty("upper", upper);
			data.addProperty("upperColor", upperColor);
			data.addProperty("lower", lower);
			data.addProperty("lowerColor", lowerColor);
			data.addProperty("chest", chest);
			data.addProperty("chestColor", chestColor);
			data.addProperty("waist", waist);
			data.addProperty("waistColor", waistColor);
			data.addProperty("hands", hands);
			data.addProperty("handsColor", handsColor);
			data.addProperty("feet", feet);
			data.addProperty("feetColor", feetColor);
			data.addProperty("accessory1", accessory1);
			data.addProperty("accessory1Color", accessory1Color);
			data.addProperty("accessory2", accessory2);
			data.addProperty("accessory2Color", accessory2Color);
			data.addProperty("facePaint", facePaint);

			JsonObject response = Campbell.instance().getResponse("accounts", "createCharacter", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while creating character: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x3102, 2);
				return;
			}

			int charId = response.get("chara").getAsInt();

			bo = ctx.alloc().directBuffer(8);
			bo.writeInt(0).writeInt(charId);

			Packets.write(ctx, 0x3102, bo);
		} catch (Exception e) {
			logger.error("Exception while creating character.", e);
			Packets.writeError(ctx, 0x3102, 1);
			Util.safeRelease(bo);
		}
	}

	public static void selectCharacter(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("index", index);

			JsonObject response = Campbell.instance().getResponse("accounts", "selectCharacter", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while selecting character: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x3104, 2);
				return;
			}

			Packets.write(ctx, 0x3104, 0);
		} catch (Exception e) {
			logger.error("Exception while selecting character.", e);
			Packets.writeError(ctx, 0x3104, 1);
		}
	}

	public static void deleteCharacter(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("index", index);

			JsonObject response = Campbell.instance().getResponse("accounts", "deleteCharacter", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while deleting character: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x3106, 2);
				return;
			}

			Packets.write(ctx, 0x3106, 0);
		} catch (Exception e) {
			logger.error("Exception while deleting character.", e);
			Packets.writeError(ctx, 0x3106, 1);
		}
	}

	public static boolean onLobbyConnected(ChannelHandlerContext ctx, int lobbyId, User user) {
		try {
			if (!NUsers.initialize(ctx.channel(), user)) {
				logger.error("Failed to initialize player during lobby connection.");
				return false;
			}
		} catch (Exception e) {
			logger.error("Exception while handling lobby connection.", e);
		}
		return true;
	}

	public static void onLobbyDisconnected(ChannelHandlerContext ctx, int lobbyId) {
		try {
			// TODO: Clean up anything we have to, like the player's rooms? Be
			// sure to make sure rooms aren't disappeaing.

			if (!NUsers.finalize(ctx.channel())) {
				logger.error("Failed to finalize user during lobby disconnection.");
				return;
			}
		} catch (Exception e) {
			logger.error("Exception while handling lobby disconnection.", e);
		}
	}

}
