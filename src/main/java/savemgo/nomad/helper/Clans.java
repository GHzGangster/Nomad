package savemgo.nomad.helper;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Clan;
import savemgo.nomad.entity.ClanMember;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.MessageClanApplication;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NGames;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Clans {

	private static final Logger logger = LogManager.getLogger();

	public static void getEmblem(ChannelHandlerContext ctx, Packet in, int command, boolean getWip) {
		ByteBuf bo = null;
		Session session = null;
		try {
			ByteBuf bi = in.getPayload();
			int clanId = bi.readInt();

			session = DB.getSession();
			session.beginTransaction();

			Clan clan = session.get(Clan.class, clanId);

			session.getTransaction().commit();
			DB.closeSession(session);

			if (clan == null) {
				Packets.write(ctx, command, Error.CLAN_DOESNOTEXIST);
				return;
			}

			bo = ctx.alloc().directBuffer(4 + 565);
			bo.writeInt(0);

			if (getWip && clan.getEmblemWip() != null) {
				bo.writeBytes(clan.getEmblemWip());
			} else if (clan.getEmblem() != null) {
				bo.writeBytes(clan.getEmblem());
			} else {
				bo.writeZero(565);
			}

			Packets.write(ctx, command, bo);
		} catch (Exception e) {
			logger.error("Exception while getting clan emblem.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, command, Error.GENERAL);
		}
	}

	public static void getList(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			// In: 00000000 0001

			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery("from Clan c join fetch c.leader l join fetch l.character",
					Clan.class);
			List<Clan> clans = query.list();

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.handleMutliElementPayload(ctx, clans.size(), 15, 48, payloads, (i, bo) -> {
				Clan clan = clans.get(i);

				boolean isNew = false;
				int time = (int) Instant.now().getEpochSecond();

				bo.writeInt(clan.getId());
				Util.writeString(clan.getName(), 16, bo);
				bo.writeInt(clan.getLeader().getCharacterId());
				Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
				bo.writeBoolean(isNew).writeByte(0).writeByte(0).writeByte(0).writeInt(time);
			});

			Packets.write(ctx, 0x4b11, 0);
			Packets.write(ctx, 0x4b12, payloads);
			Packets.write(ctx, 0x4b13, 0);
		} catch (Exception e) {
			logger.error("Exception while getting clan list.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffers(payloads);
			Packets.write(ctx, 0x4b11, Error.GENERAL);
		}
	}

	public static void search(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			ByteBuf bi = in.getPayload();
			boolean exactOnly = bi.readBoolean();
			@SuppressWarnings("unused")
			boolean caseSensitive = bi.readBoolean();
			String name = Util.readString(bi, 0x10);

			if (!exactOnly) {
				name = "%" + name + "%";
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery(
					"from Clan c join fetch c.leader l join fetch l.character where c.name like :name", Clan.class);
			query.setParameter("name", name);
			List<Clan> clans = query.list();

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.handleMutliElementPayload(ctx, clans.size(), 15, 48, payloads, (i, bo) -> {
				Clan clan = clans.get(i);

				boolean isNew = false;
				int time = (int) Instant.now().getEpochSecond();

				bo.writeInt(clan.getId());
				Util.writeString(clan.getName(), 16, bo);
				bo.writeInt(clan.getLeader().getCharacterId());
				Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
				bo.writeBoolean(isNew).writeByte(0).writeByte(0).writeByte(0).writeInt(time);
			});

			Packets.write(ctx, 0x4b91, 0);
			Packets.write(ctx, 0x4b92, payloads);
			Packets.write(ctx, 0x4b93, 0);
		} catch (Exception e) {
			logger.error("Exception while searching for clan.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffers(payloads);
			Packets.write(ctx, 0x4b91, Error.GENERAL);
		}
	}

	public static void getInformationMember(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting clan information (member): No User.");
				Packets.write(ctx, 0x4b21, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			ByteBuf bi = in.getPayload();
			int clanId = bi.readInt();

			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery(
					"from Clan c join fetch c.leader l join fetch l.character where c.id=:clanId", Clan.class);
			query.setParameter("clanId", clanId);

			Clan clan = query.uniqueResult();

			if (clan != null) {
				Hibernate.initialize(clan.getEmblemEditor());
				Hibernate.initialize(clan.getApplications());
				Hibernate.initialize(clan.getNoticeWriter());
				Hibernate.initialize(clan.getNoticeWriter());
				if (clan.getNoticeWriter() != null) {
					Hibernate.initialize(clan.getNoticeWriter().getCharacter());
				}
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			if (clan == null) {
				Packets.write(ctx, 0x4b21, Error.CLAN_DOESNOTEXIST);
				return;
			}

			int gradePoints = clan.getId();

			bo = ctx.alloc().directBuffer(777);

			bo.writeInt(0).writeInt(clan.getId());
			Util.writeString(clan.getName(), 16, bo);
			bo.writeByte(0);

			if (clan.getLeader() != null) {
				bo.writeInt(clan.getLeader().getCharacterId());
				Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);
			} else {
				bo.writeInt(0);
				Util.writeString("", 16, bo);
			}

			bo.writeInt(0);
			Util.writeString("", 16, bo);
			bo.writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0).writeInt(0)
					.writeByte(0).writeByte(0);

			if (clan.getEmblemWip() != null) {
				bo.writeByte(2);
			} else {
				bo.writeByte(0);
			}

			if (clan.getEmblem() != null) {
				bo.writeByte(3);
			} else {
				bo.writeByte(0);
			}

			if (clan.getComment() != null) {
				Util.writeString(clan.getComment(), 128, bo);
			} else {
				Util.writeString("No comment.", 128, bo);
			}

			if (clanMember != null) {
				if (clan.getEmblemEditor() != null) {
					bo.writeInt(clan.getEmblemEditor().getCharacterId());
				} else if (clan.getLeader() != null) {
					bo.writeInt(clan.getLeader().getCharacterId());
				}
			} else {
				bo.writeInt(0);
			}

			if (clan.getNotice() != null) {
				Util.writeString(clan.getNotice(), 512, bo);
				bo.writeInt(clan.getNoticeTime());
				if (clan.getNoticeWriter() != null) {
					Util.writeString(clan.getNoticeWriter().getCharacter().getName(), 16, bo);
				} else {
					Util.writeString("[Deleted]", 16, bo);
				}
			} else {
				Util.writeString("", 512, bo);
				bo.writeInt(0);
				Util.writeString("", 16, bo);
			}

			bo.writeInt(0).writeInt(0).writeInt(gradePoints);

			Packets.write(ctx, 0x4b21, bo);
		} catch (Exception e) {
			logger.error("Exception while getting clan information (member).", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4b21, Error.GENERAL);
		}
	}

	public static void getInformation(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting clan information: No User.");
				Packets.write(ctx, 0x4b81, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = character.getClanMember().size() > 0 ? character.getClanMember().get(0) : null;

			ByteBuf bi = in.getPayload();
			int clanId = bi.readInt();

			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery(
					"from Clan c join fetch c.leader l join fetch l.character join fetch c.members where c.id=:clanId",
					Clan.class);
			query.setParameter("clanId", clanId);

			Clan clan = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (clan == null) {
				Packets.write(ctx, 0x4b81, Error.CLAN_DOESNOTEXIST);
				return;
			}

			List<ClanMember> members = clan.getMembers();

			int totalReward = clan.getId();
			int gradePoints = 0, tournament = 0, firstPlace = 0, secondPlace = 0, bestFour = 0, survival = 0, win = 0,
					straightWins = 0;

			bo = ctx.alloc().directBuffer(217);

			bo.writeInt(0).writeInt(clan.getId());
			Util.writeString(clan.getName(), 16, bo);
			bo.writeInt(clan.getLeader().getCharacter().getId());
			Util.writeString(clan.getLeader().getCharacter().getName(), 16, bo);

			if (clan.getEmblem() != null) {
				bo.writeByte(3);
			} else {
				bo.writeByte(0);
			}

			if (clan.getComment() != null) {
				Util.writeString(clan.getComment(), 128, bo);
			} else {
				Util.writeString("No comment.", 128, bo);
			}

			bo.writeInt(gradePoints).writeInt(members.size()).writeInt(totalReward).writeInt(tournament)
					.writeInt(firstPlace).writeInt(secondPlace).writeInt(bestFour).writeInt(0).writeInt(survival)
					.writeInt(win).writeInt(straightWins);

			Packets.write(ctx, 0x4b81, bo);
		} catch (Exception e) {
			logger.error("Exception while getting clan information.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4b81, Error.GENERAL);
		}
	}

	public static void getRoster(ChannelHandlerContext ctx, Packet in) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		AtomicReference<ByteBuf[]> payloads2 = new AtomicReference<>();
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting clan roster: No User.");
				Packets.write(ctx, 0x4b53, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			ByteBuf bi = in.getPayload();
			int clanId = bi.readInt();
			boolean isInitialRoster = bi.readByte() == 1;

			session = DB.getSession();
			session.beginTransaction();

			Query<Clan> query = session.createQuery(
					"from Clan c join fetch c.members m join fetch m.character where c.id = :clan", Clan.class);
			query.setParameter("clan", clanId);

			Clan clan = query.uniqueResult();

			if (clan != null && clan.getLeader() != null && clanMember != null
					&& clanMember.getId().equals(clan.getLeader().getId())) {
				Query<MessageClanApplication> queryM = session.createQuery(
						"from MessageClanApplication m join fetch m.character where m.clan = :clan",
						MessageClanApplication.class);
				queryM.setParameter("clan", clan);
				clan.setApplications(queryM.list());
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			if (clan == null) {
				Packets.write(ctx, 0x4b53, Error.CLAN_DOESNOTEXIST);
				return;
			}

			List<ClanMember> members = clan.getMembers();

			Packets.handleMutliElementPayload(ctx, members.size(), 15, 68, payloads, (i, bo) -> {
				ClanMember member = members.get(i);
				Character cCharacter = member.getCharacter();

				Game game = NGames.get((g) -> {
					for (Player player : g.getPlayers()) {
						if (player.getCharacterId().equals(cCharacter.getId())) {
							return true;
						}
					}
					return false;
				});

				boolean isMember = true;
				int rewards = cCharacter.getId();

				bo.writeInt(cCharacter.getId());
				Util.writeString(cCharacter.getName(), 16, bo);
				bo.writeBoolean(isMember).writeInt(0).writeInt(rewards);

				if (game != null) {
					bo.writeShort(game.getLobbyId());
					Util.writeString(game.getLobby().getName(), 16, bo);
					bo.writeInt(game.getId());
					Util.writeString(game.getHost().getName(), 16, bo);
					bo.writeByte(game.getLobby().getSubtype());
				} else {
					bo.writeShort(0);
					Util.writeString("", 16, bo);
					bo.writeInt(0);
					Util.writeString("", 16, bo);
					bo.writeByte(0);
				}
			});

			if (clanMember != null && clan.getLeader() != null && clanMember.getId().equals(clan.getLeader().getId())) {
				List<MessageClanApplication> applications = clan.getApplications();

				Packets.handleMutliElementPayload(ctx, applications.size(), 15, 68, payloads2, (i, bo) -> {
					MessageClanApplication application = applications.get(i);
					Character cCharacter = application.getCharacter();

					Game game = NGames.get((g) -> {
						for (Player player : g.getPlayers()) {
							if (player.getCharacterId().equals(cCharacter.getId())) {
								return true;
							}
						}
						return false;
					});

					boolean isMember = false;
					int rewards = cCharacter.getId();

					bo.writeInt(cCharacter.getId());
					Util.writeString(cCharacter.getName(), 16, bo);
					bo.writeBoolean(isMember).writeInt(0).writeInt(rewards);

					if (game != null) {
						bo.writeShort(game.getLobbyId());
						Util.writeString(game.getLobby().getName(), 16, bo);
						bo.writeInt(game.getId());
						Util.writeString(game.getHost().getName(), 16, bo);
						bo.writeByte(game.getLobby().getSubtype());
					} else {
						bo.writeShort(0);
						Util.writeString("", 16, bo);
						bo.writeInt(0);
						Util.writeString("", 16, bo);
						bo.writeByte(0);
					}
				});
			}

			Packets.write(ctx, 0x4b53, 0);
			Packets.write(ctx, 0x4b54, payloads);
			Packets.write(ctx, 0x4b54, payloads2);
			Packets.write(ctx, 0x4b55, 0);
		} catch (Exception e) {
			logger.error("Exception while getting clan roster.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffers(payloads);
			Util.releaseBuffers(payloads2);
			Packets.write(ctx, 0x4b53, Error.GENERAL);
		}
	}

	public static void updateState(ChannelHandlerContext ctx, Packet in) {
		ByteBuf[] payloads = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting clan info: No User.");
				Packets.write(ctx, 0x4b47, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			MessageClanApplication clanApplication = Util.getFirstOrNull(character.getClanApplication());
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			if (clanMember != null) {
				Clan clan = clanMember.getClan();

				payloads = new ByteBuf[2];
				for (int i = 0; i < payloads.length; i++) {
					ByteBuf bo = ctx.alloc().directBuffer(28);

					bo.writeInt(0).writeInt(clan.getId());

					if (clanMember.getId().equals(clan.getLeaderId())) {
						bo.writeByte(2);
					} else {
						bo.writeByte(1);
					}

					int notifications = 0;

					if (clanMember.getId().equals(clan.getLeaderId())) {
						session = DB.getSession();
						session.beginTransaction();

						Query<MessageClanApplication> query = session.createQuery(
								"from MessageClanApplication a where a.clan = :clan", MessageClanApplication.class);
						query.setParameter("clan", clan);

						List<MessageClanApplication> applications = query.list();

						session.getTransaction().commit();
						DB.closeSession(session);

						if (applications.size() > 0) {
							notifications |= 0b100000000;
						}
					}

					bo.writeShort(notifications);

					if (clan.getEmblem() != null) {
						bo.writeByte(3);
					} else {
						bo.writeByte(0);
					}

					Util.writeString(clan.getName(), 16, bo);

					payloads[i] = bo;
				}
			} else if (clanApplication != null) {
				Clan clan = clanApplication.getClan();

				payloads = new ByteBuf[2];
				for (int i = 0; i < payloads.length; i++) {
					ByteBuf bo = ctx.alloc().directBuffer(28);

					bo.writeInt(0).writeInt(clan.getId());

					bo.writeByte(0);

					bo.writeShort(0);

					if (clan.getEmblem() != null) {
						bo.writeByte(3);
					} else {
						bo.writeByte(0);
					}

					Util.writeString(clan.getName(), 16, bo);

					payloads[i] = bo;
				}
			} else {
				payloads = new ByteBuf[1];
				for (int i = 0; i < payloads.length; i++) {
					ByteBuf bo = ctx.alloc().directBuffer(28);

					bo.writeInt(0).writeInt(0);

					bo.writeByte(0xff);

					bo.writeShort(0);

					bo.writeByte(0);

					Util.writeString("", 16, bo);

					payloads[i] = bo;
				}
			}

			Packets.write(ctx, 0x4b47, payloads);
		} catch (Exception e) {
			logger.error("Exception while updating clan state.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffers(payloads);
			Packets.write(ctx, 0x4b47, Error.GENERAL);
		}
	}

	public static void updateComment(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating comment: No User.");
				Packets.write(ctx, 0x4b65, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			String comment = Util.readString(bi, 128);

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = character.getClanMember().size() > 0 ? character.getClanMember().get(0) : null;

			if (clanMember == null) {
				Packets.write(ctx, 0x4b65, Error.CLAN_NOTAMEMBER);
				return;
			}

			Clan clan = clanMember.getClan();
			if (clan == null) {
				Packets.write(ctx, 0x4b65, Error.CLAN_DOESNOTEXIST);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Clan sClan = session.get(Clan.class, clan.getId());
			sClan.setComment(comment);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b65, 0);
		} catch (Exception e) {
			logger.error("Exception while updating comment.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b65, Error.GENERAL);
		}
	}

	public static void updateNotice(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating notice: No User.");
				Packets.write(ctx, 0x4b67, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			String comment = Util.readString(bi, 512);

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();

			ClanMember clanMember = character.getClanMember().size() > 0 ? character.getClanMember().get(0) : null;
			if (clanMember == null) {
				Packets.write(ctx, 0x4b67, Error.CLAN_NOTAMEMBER);
				return;
			}

			Clan clan = clanMember.getClan();
			if (clan == null) {
				Packets.write(ctx, 0x4b67, Error.CLAN_DOESNOTEXIST);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Clan sClan = session.get(Clan.class, clan.getId());
			sClan.setNotice(comment);
			sClan.setNoticeTime((int) Instant.now().getEpochSecond());
			sClan.setNoticeWriter(clanMember);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b67, 0);
		} catch (Exception e) {
			logger.error("Exception while updating clan notice.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b67, Error.GENERAL);
		}
	}

	public static void getStats(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bo1 = Util.readFile(new File("test/4b71.bin"));
			ByteBuf bo2 = Util.readFile(new File("test/4b72.bin"));
			Packets.write(ctx, 0x4b71, bo1);
			Packets.write(ctx, 0x4b72, bo2);
		} catch (Exception e) {
			logger.error("Exception while getting clan stats.", e);
			Packets.write(ctx, 0x4b71, Error.GENERAL);
		}
	}

	public static void create(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while creating clan: No User.");
				Packets.write(ctx, 0x4b01, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			String name = Util.readString(bi, 16);
			String comment = Util.readString(bi, 128);

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			if (clanMember != null) {
				logger.error("Error while creating clan: Currently in a clan.");
				Packets.write(ctx, 0x4b01, Error.CLAN_INACLAN);
				return;
			}

			Clan clan = new Clan();
			clan.setName(name);
			clan.setComment(comment);
			clan.setOpen(1);

			clanMember = new ClanMember();
			clanMember.setCharacter(character);
			clanMember.setClan(clan);

			clan.setLeader(clanMember);

			session = DB.getSession();
			session.beginTransaction();

			session.save(clan);
			session.save(clanMember);

			session.getTransaction().commit();
			DB.closeSession(session);

			List<ClanMember> clanMembers = new ArrayList<>();
			clanMembers.add(clanMember);
			character.setClanMember(clanMembers);

			Packets.write(ctx, 0x4b01, 0);
		} catch (Exception e) {
			logger.error("Exception while creating clan.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b01, Error.GENERAL);
		}
	}

	public static void apply(ChannelHandlerContext ctx, Packet in) {
		Packets.write(ctx, 0x4b43, 0);
	}

	public static void acceptJoin(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while accepting join: No User.");
				Packets.write(ctx, 0x4b31, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetCharaId = bi.readInt();

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			Clan clan = clanMember != null ? clanMember.getClan() : null;

			if (clanMember == null) {
				logger.error("Error while accepting join: Not in a clan.");
				Packets.write(ctx, 0x4b31, Error.CLAN_NOTAMEMBER);
				return;
			}

			if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
				logger.error("Error while accepting join: Not a clan leader.");
				Packets.write(ctx, 0x4b31, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> queryC = session.createQuery(
					"from Character c join fetch c.clanApplication where c.id = :character", Character.class);
			queryC.setParameter("character", targetCharaId);

			Character targetCharacter = queryC.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (targetCharacter == null) {
				logger.error("Error while accepting join: Target does not exist.");
				Packets.write(ctx, 0x4b31, Error.CHARACTER_DOESNOTEXIST);
				return;
			}

			MessageClanApplication application = Util.getFirstOrNull(targetCharacter.getClanApplication());

			if (application == null) {
				logger.error("Error while accepting join: Target has not applied.");
				Packets.write(ctx, 0x4b31, Error.CLAN_NOAPPLICATION);
				return;
			}

			if (!application.getClanId().equals(clan.getId())) {
				logger.error("Error while accepting join: Not the same clan.");
				Packets.write(ctx, 0x4b31, Error.CLAN_NOTALEADER);
				return;
			}

			ClanMember targetClanMember = new ClanMember();
			targetClanMember.setCharacter(targetCharacter);
			targetClanMember.setClan(clan);

			session = DB.getSession();
			session.beginTransaction();

			session.remove(application);
			session.save(targetClanMember);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b31, 0);
		} catch (Exception e) {
			logger.error("Exception while accepting join.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b31, Error.GENERAL);
		}
	}

	public static void declineJoin(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while declining join: No User.");
				Packets.write(ctx, 0x4b33, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetCharaId = bi.readInt();

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			Clan clan = clanMember != null ? clanMember.getClan() : null;

			if (clanMember == null) {
				logger.error("Error while accepting join: Not in a clan.");
				Packets.write(ctx, 0x4b33, Error.CLAN_NOTAMEMBER);
				return;
			}

			if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
				logger.error("Error while accepting join: Not a clan leader.");
				Packets.write(ctx, 0x4b33, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<Character> queryC = session.createQuery(
					"from Character c join fetch c.clanApplication where c.id = :character", Character.class);
			queryC.setParameter("character", targetCharaId);

			Character targetCharacter = queryC.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (targetCharacter == null) {
				logger.error("Error while accepting join: Target does not exist.");
				Packets.write(ctx, 0x4b33, Error.CHARACTER_DOESNOTEXIST);
				return;
			}

			MessageClanApplication application = Util.getFirstOrNull(targetCharacter.getClanApplication());

			if (application == null) {
				logger.error("Error while accepting join: Target has not applied.");
				Packets.write(ctx, 0x4b33, Error.CLAN_NOAPPLICATION);
				return;
			}

			if (!application.getClanId().equals(clan.getId())) {
				logger.error("Error while accepting join: Not the same clan.");
				Packets.write(ctx, 0x4b33, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			session.remove(application);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b33, 0);
		} catch (Exception e) {
			logger.error("Exception while declining join.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b33, Error.GENERAL);
		}
	}

	public static void leave(ChannelHandlerContext ctx) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while leaving clan: No User.");
				Packets.write(ctx, 0x4b41, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			MessageClanApplication clanApplication = Util.getFirstOrNull(character.getClanApplication());

			session = DB.getSession();
			session.beginTransaction();

			if (clanApplication != null) {
				session.delete(clanApplication);

				character.getClanApplication().clear();
			}

			if (clanMember != null) {
				session.delete(clanMember);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			if (clanApplication != null) {
				character.getClanApplication().clear();
			}

			if (clanMember != null) {
				character.getClanMember().clear();
			}

			Packets.write(ctx, 0x4b41, 0);
		} catch (Exception e) {
			logger.error("Exception while leaving clan.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b41, Error.GENERAL);
		}
	}

	public static void setEmblem(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while cancelling clan application: No User.");
				Packets.write(ctx, 0x4b51, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			if (clanMember == null) {
				Packets.write(ctx, 0x4b51, Error.CLAN_NOTAMEMBER);
				return;
			}

			ByteBuf bi = in.getPayload();
			int type = bi.readByte();
			byte[] emblem = new byte[565];
			bi.readBytes(emblem);

			boolean isWip = type == 2;

			session = DB.getSession();
			session.beginTransaction();

			Clan clan = session.get(Clan.class, clanMember.getClanId());
			if (isWip) {
				clan.setEmblemWip(emblem);
			} else {
				clan.setEmblem(emblem);
			}

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b51, 0);
		} catch (Exception e) {
			logger.error("Exception while cancelling clan application.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b51, Error.GENERAL);
		}
	}

	public static void disband(ChannelHandlerContext ctx) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while disbanding clan: No User.");
				Packets.write(ctx, 0x4b05, Error.INVALID_SESSION);
				return;
			}

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());

			if (clanMember == null) {
				Packets.write(ctx, 0x4b05, Error.CLAN_NOTAMEMBER);
				return;
			}

			Clan clan = clanMember.getClan();

			if (!clan.getLeaderId().equals(clanMember.getId())) {
				Packets.write(ctx, 0x4b05, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			session.delete(clan);

			session.getTransaction().commit();
			DB.closeSession(session);

			character.getClanApplication().clear();

			Packets.write(ctx, 0x4b05, 0);
		} catch (Exception e) {
			logger.error("Exception while disbanding clan.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b05, Error.GENERAL);
		}
	}

	public static void banish(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while banishing member: No User.");
				Packets.write(ctx, 0x4b37, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetCharaId = bi.readInt();

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			Clan clan = clanMember != null ? clanMember.getClan() : null;

			if (clanMember == null) {
				logger.error("Error while banishing member: Not in a clan.");
				Packets.write(ctx, 0x4b37, Error.CLAN_NOTAMEMBER);
				return;
			}

			if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
				logger.error("Error while banishing member: Not a clan leader.");
				Packets.write(ctx, 0x4b37, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<ClanMember> query = session.createQuery(
					"from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
			query.setParameter("character", targetCharaId);

			ClanMember targetClanMember = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (targetClanMember == null) {
				logger.error("Error while accepting join: Target does not exist.");
				Packets.write(ctx, 0x4b37, Error.CHARACTER_DOESNOTEXIST);
				return;
			}

			if (!targetClanMember.getClanId().equals(clan.getId())) {
				logger.error("Error while accepting join: Not the same clan.");
				Packets.write(ctx, 0x4b37, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			session.remove(targetClanMember);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b37, 0);
		} catch (Exception e) {
			logger.error("Exception while banishing member.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b37, Error.GENERAL);
		}
	}

	public static void transferLeadership(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while tranferring leadership: No User.");
				Packets.write(ctx, 0x4b61, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetCharaId = bi.readInt();

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			Clan clan = clanMember != null ? clanMember.getClan() : null;

			if (clanMember == null) {
				logger.error("Error while tranferring leadership: Not in a clan.");
				Packets.write(ctx, 0x4b61, Error.CLAN_NOTAMEMBER);
				return;
			}

			if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
				logger.error("Error while tranferring leadership: Not a clan leader.");
				Packets.write(ctx, 0x4b61, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<ClanMember> query = session.createQuery(
					"from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
			query.setParameter("character", targetCharaId);

			ClanMember targetClanMember = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (targetClanMember == null) {
				logger.error("Error while tranferring leadership: Target does not exist.");
				Packets.write(ctx, 0x4b61, Error.CHARACTER_DOESNOTEXIST);
				return;
			}

			if (!targetClanMember.getClanId().equals(clan.getId())) {
				logger.error("Error while tranferring leadership: Not the same clan.");
				Packets.write(ctx, 0x4b61, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			clan.setLeader(targetClanMember);
			session.update(clan);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b61, 0);
		} catch (Exception e) {
			logger.error("Exception while tranferring leadership.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b61, Error.GENERAL);
		}
	}

	public static void setEmblemEditor(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while assigning emblem rights: No User.");
				Packets.write(ctx, 0x4b63, Error.INVALID_SESSION);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetCharaId = bi.readInt();

			Users.updateUserClan(ctx);

			Character character = user.getCurrentCharacter();
			ClanMember clanMember = Util.getFirstOrNull(character.getClanMember());
			Clan clan = clanMember != null ? clanMember.getClan() : null;

			if (clanMember == null) {
				logger.error("Error while assigning emblem rights: Not in a clan.");
				Packets.write(ctx, 0x4b63, Error.CLAN_NOTAMEMBER);
				return;
			}

			if (clan.getLeader() == null || !clanMember.getId().equals(clan.getLeader().getId())) {
				logger.error("Error while assigning emblem rights: Not a clan leader.");
				Packets.write(ctx, 0x4b63, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			Query<ClanMember> query = session.createQuery(
					"from ClanMember m join fetch m.character c where c.id = :character", ClanMember.class);
			query.setParameter("character", targetCharaId);

			ClanMember targetClanMember = query.uniqueResult();

			session.getTransaction().commit();
			DB.closeSession(session);

			if (targetClanMember == null) {
				logger.error("Error while assigning emblem rights: Target does not exist.");
				Packets.write(ctx, 0x4b63, Error.CHARACTER_DOESNOTEXIST);
				return;
			}

			if (!targetClanMember.getClanId().equals(clan.getId())) {
				logger.error("Error while assigning emblem rights: Not the same clan.");
				Packets.write(ctx, 0x4b63, Error.CLAN_NOTALEADER);
				return;
			}

			session = DB.getSession();
			session.beginTransaction();

			if (clanMember.getCharacterId().equals(targetCharaId)) {
				clan.setEmblemEditor(null);
			} else {
				clan.setEmblemEditor(targetClanMember);
			}

			session.update(clan);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4b63, 0);
		} catch (Exception e) {
			logger.error("Exception while assigning emblem rights.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4b63, Error.GENERAL);
		}
	}

}
