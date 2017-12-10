package savemgo.nomad.helper;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Clan;
import savemgo.nomad.entity.ClanMember;
import savemgo.nomad.entity.MessageClanApplication;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Messages {

	private static final Logger logger = LogManager.getLogger(Messages.class);

	public static void getMessages(ChannelHandlerContext ctx, Packet in) {
		ByteBuf[] payloads = new ByteBuf[0];
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting messages: No User.");
				Packets.write(ctx, 0x4821, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int type = bi.readByte();

			if (type == 0xf) {
				// Mail

				Packets.write(ctx, 0x4821, 0);
				Packets.write(ctx, 0x4823, 0);
			} else if (type == 0x10) {
				// Clan Applications

				Users.updateUserClan(ctx);

				Character character = user.getCurrentCharacter();
				if (character == null) {
					logger.error("Error while getting messages: No character.");
					Packets.write(ctx, 0x4821, Error.INVALID_SESSION);
					return;
				}

				ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
				if (clanMember == null) {
					logger.error("Error while getting messages: No clan member.");
					Packets.write(ctx, 0x4821, Error.CLAN_NOTAMEMBER);
					return;
				}

				Clan clan = clanMember.getClan();

				session = DB.getSession();
				session.beginTransaction();

				Query<MessageClanApplication> query = session.createQuery(
						"from MessageClanApplication m join fetch m.character where m.clan = :clan",
						MessageClanApplication.class);
				query.setParameter("clan", clan);
				List<MessageClanApplication> messages = query.list();

				session.getTransaction().commit();
				DB.closeSession(session);

				payloads = new ByteBuf[messages.size()];
				for (int i = 0; i < payloads.length; i++) {
					ByteBuf bo = ctx.alloc().directBuffer(266);
					MessageClanApplication message = messages.get(i);

					int mtype = 0;
					boolean important = false;
					boolean read = false;

					int unk1 = 1;
					int unk2 = 0;

					bo.writeByte(mtype).writeByte(i).writeByte(unk1);
					Util.writeString(message.getCharacter().getName(), 128, bo);
					Util.writeString(message.getComment(), 128, bo);
					bo.writeInt(message.getTime()).writeByte(unk2).writeBoolean(important).writeBoolean(read);

					payloads[i] = bo;
				}

				Packets.write(ctx, 0x4821, 0);
				Packets.write(ctx, 0x4822, payloads);
				Packets.write(ctx, 0x4823, 0);
			}
		} catch (Exception e) {
			logger.error("Exception while getting messages.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4821, Error.GENERAL);
			Util.releaseBuffers(payloads);
		}
	}

	public static void getContents(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4341);
	}

	private static final byte[] SEND_GENERAL_ERROR = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x53, (byte) 0x65, (byte) 0x72, (byte) 0x76,
			(byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x65, (byte) 0x72, (byte) 0x72, (byte) 0x6F, (byte) 0x72,
			(byte) 0x21, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xC0, (byte) 0xFF, (byte) 0xEE, (byte) 0x01 };

	public static void send(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while sending message: No User.");
				Packets.write(ctx, 0x4801, Unpooled.wrappedBuffer(SEND_GENERAL_ERROR));
				return;
			}

			ByteBuf bi = in.getPayload();
			boolean hasName = bi.readBoolean();
			String name = Util.readString(bi, 128);
			String comment = Util.readString(bi, 128);
			bi.skipBytes(708);
			/**
			 * 0 - Character
			 * 
			 * 1 - Clan Application
			 * 
			 * 3 - Customer Support
			 */
			int recipientType = bi.readByte();
			bi.skipBytes(1);

			List<MessageRecipientError> recipientErrors = new ArrayList<>();

			if (recipientType == 1) {
				Users.updateUserClan(ctx);

				Character character = user.getCurrentCharacter();
				MessageClanApplication clanApplication = Util.getFirstOrNull(character.getClanApplication());
				ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

				if (clanApplication != null) {
					recipientErrors.add(new MessageRecipientError("Already applied!", Error.CLAN_HASAPPLICATION));
				}

				if (clanMember != null) {
					recipientErrors.add(new MessageRecipientError("Already in clan!", Error.CLAN_INACLAN));
				}
				
				if (recipientErrors.size() <= 0) {
					session = DB.getSession();
					session.beginTransaction();

					Query<Clan> query = session.createQuery("from Clan c where c.name = :name", Clan.class);
					query.setParameter("name", name);
					Clan clan = query.uniqueResult();

					session.getTransaction().commit();
					DB.closeSession(session);
					
					if (clan == null) {
						recipientErrors.add(new MessageRecipientError("Bad clan name!", Error.CLAN_DOESNOTEXIST));
					}
					
					if (recipientErrors.size() <= 0) {
						session = DB.getSession();
						session.beginTransaction();

						MessageClanApplication message = new MessageClanApplication();
						message.setCharacter(character);
						message.setClan(clan);
						message.setComment(comment);
						message.setTime((int) Instant.now().getEpochSecond());

						session.save(message);

						session.getTransaction().commit();
						DB.closeSession(session);
					}
				}
			} else {
				recipientErrors.add(new MessageRecipientError("Not implemented!", Error.NOT_IMPLEMENTED));
			}

			bo = ctx.alloc().directBuffer(9 + recipientErrors.size() * 20);

			int unk2 = 0;

			if (recipientErrors.size() > 0) {
				bo.writeInt(1);
			} else {
				bo.writeInt(0);
			}
			
			bo.writeByte(unk2).writeInt(recipientErrors.size());

			for (MessageRecipientError recipientError : recipientErrors) {
				Util.writeString(recipientError.getRecipient(), 16, bo);
				int code = recipientError.getError().getCode();
				if (!recipientError.getError().isOfficial()) {
					code |= 0xC0FFEE << 8;
				}
				bo.writeInt(code);
			}

			Packets.write(ctx, 0x4801, bo);
		} catch (Exception e) {
			logger.error("Exception while sending message.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4801, Unpooled.wrappedBuffer(SEND_GENERAL_ERROR));
		}
	}

	public static void addSent(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while adding sent message.");
				Packets.write(ctx, 0x4861, Error.INVALID_SESSION);
				return;
			}

			Packets.write(ctx, 0x4861, 0);
		} catch (Exception e) {
			logger.error("Exception while adding sent message.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4861, Error.GENERAL);
		}
	}

	static class MessageRecipientError {

		private String recipient;
		private Error error;

		public MessageRecipientError(String recipient, Error error) {
			this.recipient = recipient;
			this.error = error;
		}

		public String getRecipient() {
			return recipient;
		}

		public void setRecipient(String recipient) {
			this.recipient = recipient;
		}

		public Error getError() {
			return error;
		}

		public void setError(Error error) {
			this.error = error;
		}

	}

}
