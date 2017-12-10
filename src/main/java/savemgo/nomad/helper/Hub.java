package savemgo.nomad.helper;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.News;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NLobbies;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Hub {

	private static final Logger logger = LogManager.getLogger(Hub.class);

	public static void getGameLobbyInfo(ChannelHandlerContext ctx) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			Collection<Lobby> lobbies = NLobbies.get().values();
			Iterator<Lobby> iterator = lobbies.iterator();

			Packets.handleMutliElementPayload(ctx, lobbies.size(), 8, 0x23, payloads, (i, bo) -> {
				Lobby lobby = iterator.next();

				int unk1 = 0;

				int attributes = 0;
				attributes |= unk1;
				attributes |= (lobby.getSubtype() & 0xff) << 24;
				int openTime = 0, closeTime = 0, isOpen = 1;

				bo.writeInt(i).writeInt(attributes).writeShort(lobby.getId());
				Util.writeString(lobby.getName(), 16, bo);
				bo.writeInt(openTime).writeInt(closeTime).writeByte(isOpen);
			});

			Packets.write(ctx, 0x4901, 0);
			Packets.write(ctx, 0x4902, payloads);
			Packets.write(ctx, 0x4903, 0);
		} catch (Exception e) {
			logger.error("Exception while getting game lobby info.", e);
			Util.releaseBuffers(payloads);
			Packets.write(ctx, 0x4901, Error.GENERAL);
		}
	}

	public static void getLobbyList(ChannelHandlerContext ctx) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			Collection<Lobby> lobbies = NLobbies.get().values();
			Iterator<Lobby> iterator = lobbies.iterator();

			Packets.handleMutliElementPayload(ctx, lobbies.size(), 22, 0x2e, payloads, (i, bo) -> {
				Lobby lobby = iterator.next();

				boolean beginner = false, expansion = false, noHeadshot = false;

				int restriction = 0;
				restriction |= beginner ? 0b1 : 0;
				restriction |= expansion ? 0b1000 : 0;
				restriction |= noHeadshot ? 0b10000 : 0;

				bo.writeInt(i).writeInt(lobby.getType());
				Util.writeString(lobby.getName(), 16, bo);
				Util.writeString(lobby.getIp(), 15, bo);
				bo.writeShort(lobby.getPort()).writeShort(lobby.getPlayers()).writeShort(lobby.getId())
						.writeByte(restriction);
			});

			Packets.write(ctx, 0x2002, 0);
			Packets.write(ctx, 0x2003, payloads);
			Packets.write(ctx, 0x2004, 0);
		} catch (Exception e) {
			logger.error("Exception while getting lobby list.", e);
			Util.releaseBuffers(payloads);
			Packets.write(ctx, 0x2002, Error.GENERAL);
		}
	}

	public static void getNews(ChannelHandlerContext ctx) {
		// In: 01
		ByteBuf[] bos = null;
		Session session = null;
		try {
			session = DB.getSession();
			session.beginTransaction();

			Query<News> query = session.createQuery("from News n order by n.id desc", News.class);
			List<News> news = query.list();

			session.getTransaction().commit();
			DB.closeSession(session);

			int newsItems = news.size();

			bos = new ByteBuf[newsItems];

			for (int i = 0; i < newsItems; i++) {
				News newsItem = news.get(i);

				String message = newsItem.getMessage();

				int length = Math.min(message.length(), Packet.MAX_PAYLOAD_LENGTH - 138);
				message = message.substring(0, length);

				bos[i] = ctx.alloc().directBuffer(138 + length);
				ByteBuf bo = bos[i];

				bo.writeInt(newsItem.getId()).writeBoolean(newsItem.getImportant()).writeInt(newsItem.getTime());
				Util.writeString(newsItem.getTopic(), 128, bo);
				Util.writeString(message, length + 1, bo);
			}

			Packets.write(ctx, 0x2009, 0);
			Packets.write(ctx, 0x200a, bos);
			Packets.write(ctx, 0x200b, 0);
		} catch (Exception e) {
			logger.error("Exception while getting news.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffers(bos);
			Packets.write(ctx, 0x2009, Error.GENERAL);
		}
	}

	public static void getGameEntryInfo(ChannelHandlerContext ctx) {
		ByteBuf bo = null;
		try {
			bo = ctx.alloc().directBuffer(0xac);

			bo.writeInt(0).writeInt(1).writeZero(0xa4);

			Packets.write(ctx, 0x4991, bo);
		} catch (Exception e) {
			logger.error("Exception while getting game entry info.", e);
			Packets.write(ctx, 0x4991, Error.GENERAL);
			Util.releaseBuffer(bo);
		}
	}

	public static void onLobbyDisconnect(ChannelHandlerContext ctx, Packet in) {
		// In: 00
		Packets.write(ctx, 0x4151);
	}

	private static final byte[] TRAINING_BYTES = new byte[] { (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x15,
			(byte) 0x00, (byte) 0x3A, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x61 };

	public static void onTrainingConnect(ChannelHandlerContext ctx, Packet in) {
		// In: 08
		ByteBuf bo = Unpooled.wrappedBuffer(TRAINING_BYTES);
		Packets.write(ctx, 0x43d1, bo);
	}

	public static void initializeLobbies() {
		Session session = null;
		try {
			Collection<Lobby> lobbies = NLobbies.get().values();

			session = DB.getSession();
			session.beginTransaction();

			for (Lobby lobby : lobbies) {
				Query query = session.createQuery("delete Game where lobby=:lobby");
				query.setParameter("lobby", lobby);
				query.executeUpdate();
			}

			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception while updating lobby count.", e);
		}
	}

	public static void updateLobbies() {
		Session session = null;
		try {
			Collection<Lobby> lobbies = NLobbies.get().values();
			for (Lobby lobby : lobbies) {
				List<User> users = NUsers.get((user) -> {
					try {
						Character character = user.getCurrentCharacter();
						if (character != null && character.getLobbyId() != null) {
							return character.getLobbyId() == lobby.getId();
						}
					} catch (Exception ex) {
						logger.error("Exception while updating lobby counts.", ex);
					}
					return false;
				});
				lobby.setPlayers(users.size());
				logger.debug("Updated Lobby {} : {} players.", lobby.getId(), users.size());
			}

			session = DB.getSession();
			session.beginTransaction();

			for (Lobby lobby : lobbies) {
				session.update(lobby);
			}

			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception while updating lobby count.", e);
		}
	}

}
