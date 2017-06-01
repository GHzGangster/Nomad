package savemgo.nomad.helper;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

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

public class Games {

	private static final Logger logger = LogManager.getLogger(Games.class);

	public static void getListFile(ChannelHandlerContext ctx, int lobby, int command) {
		try {
			ByteBuf bo = Util.readFile(new File("gamelist.bin"));
			Packets.write(ctx, command, 0);
			Packets.write(ctx, command + 1, bo);
			Packets.write(ctx, command + 2, 0);
		} catch (Exception e) {
			logger.error("Exception while getting game list.", e);
			Packets.writeError(ctx, command, 1);
		}
	}
	
	public static void getDetailsFile(ChannelHandlerContext ctx, Packet in, int lobby) {
		try {
			ByteBuf bo = Util.readFile(new File("gameinfo.bin"));
			Packets.write(ctx, 0x4313, bo);
		} catch (Exception e) {
			logger.error("Exception while getting game info.", e);
			Packets.writeError(ctx, 0x4313, 1);
		}
	}
	
	public static void joinHostFile(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bo = Util.readFile(new File("hostconnection.bin"));
			Packets.write(ctx, 0x4321, bo);
		} catch (Exception e) {
			logger.error("Exception while getting host connection info.", e);
			Packets.writeError(ctx, 0x4321, 1);
		}
	}

	public static void getGameList(ChannelHandlerContext ctx, int lobby, int command) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			JsonObject data = new JsonObject();
			data.addProperty("lobby", lobby);

			JsonObject response = Campbell.instance().getResponse("games", "getGameList", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting game list: " + Campbell.getResult(response));
				Packets.writeError(ctx, command, 2);
				return;
			}

			JsonArray games = response.get("games").getAsJsonArray();

			Packets.handleMutliElementPayload(ctx, games.size(), 18, 0x37, payloads, (i, bo) -> {
				JsonObject game = games.get(i).getAsJsonObject();

				int id = game.get("id").getAsInt();
				String name = game.get("name").getAsString();
				boolean locked = game.get("locked").getAsBoolean();
				int players = game.get("players").getAsInt();
				int maxPlayers = game.get("maxPlayers").getAsInt();
				int currentGame = game.get("currentGame").getAsInt();
				int stance = game.get("stance").getAsInt();
				int ping = game.get("ping").getAsInt();

				JsonObject common = game.get("common").getAsJsonObject();

				boolean dedicated = common.get("dedicated").getAsBoolean();
				int briefingTime = common.get("briefingTime").getAsInt();
				boolean nonStat = common.get("nonStat").getAsBoolean();
				boolean friendlyFire = common.get("friendlyFire").getAsBoolean();
				boolean autoAim = common.get("autoAim").getAsBoolean();

				JsonObject uniques = common.get("uniques").getAsJsonObject();
				boolean uniquesEnabled = uniques.get("enabled").getAsBoolean();
				boolean uniquesRandom = uniques.get("random").getAsBoolean();
				int uniqueRed = uniques.get("red").getAsInt();
				int uniqueBlue = uniques.get("blue").getAsInt();

				boolean enemyNametags = common.get("enemyNametags").getAsBoolean();
				boolean silentMode = common.get("silentMode").getAsBoolean();
				boolean autoAssign = common.get("autoAssign").getAsBoolean();
				boolean teamsSwitch = common.get("teamsSwitch").getAsBoolean();
				boolean ghosts = common.get("ghosts").getAsBoolean();

				JsonObject levelLimit = common.get("levelLimit").getAsJsonObject();
				boolean levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
				int levelLimitBase = levelLimit.get("base").getAsInt();
				int levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

				boolean voiceChat = common.get("voiceChat").getAsBoolean();
				int teamKillKick = common.get("teamKillKick").getAsInt();
				int idleKick = common.get("idleKick").getAsInt();

				int averageLevel = game.get("averageExperience").getAsInt();
				int hostScore = game.get("hostScore").getAsInt();
				int hostVotes = game.get("hostVotes").getAsInt();

				JsonArray jGames = game.get("games").getAsJsonArray();
				JsonArray jGame = jGames.get(currentGame).getAsJsonArray();
				int rule = jGame.get(0).getAsInt();
				int map = jGame.get(1).getAsInt();

				int hostOptions = 0;
				if (locked) {
					hostOptions += 1;
				}
				if (dedicated) {
					hostOptions += 2;
				}

				int commonA = 0x4;
				if (idleKick > 0) {
					commonA += 1;
				}
				if (friendlyFire) {
					commonA += 8;
				}
				if (ghosts) {
					commonA += 16;
				}
				if (autoAim) {
					commonA += 32;
				}
				if (uniquesEnabled) {
					commonA += 128;
				}

				int commonB = 0;
				if (teamsSwitch) {
					commonB += 1;
				}
				if (autoAssign) {
					commonB += 2;
				}
				if (silentMode) {
					commonB += 4;
				}
				if (enemyNametags) {
					commonB += 8;
				}
				if (levelLimitEnabled) {
					commonB += 16;
				}
				if (voiceChat) {
					commonB += 64;
				}
				if (teamKillKick > 0) {
					commonB += 128;
				}

				int friendBlock = 0;

				bo.writeInt(id);
				Util.writeString(name, 16, bo);
				bo.writeByte(hostOptions).writeByte(0x08).writeByte(rule).writeByte(map).writeZero(1)
						.writeByte(maxPlayers).writeByte(stance).writeByte(commonA).writeByte(commonB)
						.writeByte(players).writeInt(ping).writeByte(friendBlock).writeByte(levelLimitTolerance)
						.writeInt(levelLimitBase).writeInt(averageLevel).writeInt(hostScore).writeInt(hostVotes)
						.writeZero(2).writeByte(0x63);
			});

			Packets.write(ctx, command, 0);
			Packets.write(ctx, command + 1, payloads);
			Packets.write(ctx, command + 2, 0);
		} catch (Exception e) {
			logger.error("Exception while getting game list.", e);
			Packets.writeError(ctx, command, 1);
			Util.releaseBuffers(payloads);
		}
	}

	public static void getGameInfo(ChannelHandlerContext ctx, Packet in, int lobbyId) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int gameId = bi.readInt();

			JsonObject data = new JsonObject();
			data.addProperty("lobby", lobbyId);
			data.addProperty("game", gameId);

			JsonObject response = Campbell.instance().getResponse("games", "getGameDetails", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting game list: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4313, 2);
				return;
			}

			JsonObject details = response.get("game").getAsJsonObject();

			String name = details.get("name").getAsString();
			String comment = details.get("comment").getAsString();			
			int averageExperience = details.get("averageExperience").getAsInt();
			int hostScore = details.get("hostScore").getAsInt();
			int hostVotes = details.get("hostVotes").getAsInt();
			int maxPlayers = details.get("maxPlayers").getAsInt();
			int stance = details.get("stance").getAsInt();

			JsonArray players = details.get("players").getAsJsonArray();
			int currentPlayers = players.size();

			JsonArray games = details.get("games").getAsJsonArray();

			JsonObject common = details.get("common").getAsJsonObject();
			boolean dedicated = common.get("dedicated").getAsBoolean();
			int briefingTime = common.get("briefingTime").getAsInt();
			boolean nonStat = common.get("nonStat").getAsBoolean();
			boolean friendlyFire = common.get("friendlyFire").getAsBoolean();
			boolean autoAim = common.get("autoAim").getAsBoolean();

			JsonObject uniques = common.get("uniques").getAsJsonObject();
			boolean uniquesEnabled = uniques.get("enabled").getAsBoolean();
			boolean uniquesRandom = uniques.get("random").getAsBoolean();
			int uniqueRed = uniques.get("red").getAsInt();
			int uniqueBlue = uniques.get("blue").getAsInt();

			boolean enemyNametags = common.get("enemyNametags").getAsBoolean();
			boolean silentMode = common.get("silentMode").getAsBoolean();
			boolean autoAssign = common.get("autoAssign").getAsBoolean();
			boolean teamsSwitch = common.get("teamsSwitch").getAsBoolean();
			boolean ghosts = common.get("ghosts").getAsBoolean();

			JsonObject levelLimit = common.get("levelLimit").getAsJsonObject();
			boolean levelLimitEnabled = levelLimit.get("enabled").getAsBoolean();
			int levelLimitBase = levelLimit.get("base").getAsInt();
			int levelLimitTolerance = levelLimit.get("tolerance").getAsInt();

			boolean voiceChat = common.get("voiceChat").getAsBoolean();
			int teamKillKick = common.get("teamKillKick").getAsInt();
			int idleKick = common.get("idleKick").getAsInt();

			JsonObject weaponRestrictions = common.get("weaponRestrictions").getAsJsonObject();
			boolean weaponRestrictionEnabled = weaponRestrictions.get("enabled").getAsBoolean();

			JsonObject wrPrimary = weaponRestrictions.get("primary").getAsJsonObject();
			boolean vz = wrPrimary.get("vz").getAsBoolean();
			boolean p90 = wrPrimary.get("p90").getAsBoolean();
			boolean mp5 = wrPrimary.get("mp5").getAsBoolean();
			boolean patriot = wrPrimary.get("patriot").getAsBoolean();
			boolean ak = wrPrimary.get("ak").getAsBoolean();
			boolean m4 = wrPrimary.get("m4").getAsBoolean();
			boolean mk17 = wrPrimary.get("mk17").getAsBoolean();
			boolean xm8 = wrPrimary.get("xm8").getAsBoolean();
			boolean g3a3 = wrPrimary.get("g3a3").getAsBoolean();
			boolean svd = wrPrimary.get("svd").getAsBoolean();
			boolean mosin = wrPrimary.get("mosin").getAsBoolean();
			boolean m14 = wrPrimary.get("m14").getAsBoolean();
			boolean vss = wrPrimary.get("vss").getAsBoolean();
			boolean dsr = wrPrimary.get("dsr").getAsBoolean();
			boolean m870 = wrPrimary.get("m870").getAsBoolean();
			boolean saiga = wrPrimary.get("saiga").getAsBoolean();
			boolean m60 = wrPrimary.get("m60").getAsBoolean();
			boolean shield = wrPrimary.get("shield").getAsBoolean();
			boolean rpg = wrPrimary.get("rpg").getAsBoolean();
			boolean knife = wrPrimary.get("knife").getAsBoolean();

			JsonObject wrSecondary = weaponRestrictions.get("secondary").getAsJsonObject();
			boolean gsr = wrSecondary.get("gsr").getAsBoolean();
			boolean mk2 = wrSecondary.get("mk2").getAsBoolean();
			boolean operator = wrSecondary.get("operator").getAsBoolean();
			boolean g18 = wrSecondary.get("g18").getAsBoolean();
			boolean mk23 = wrSecondary.get("mk23").getAsBoolean();
			boolean de = wrSecondary.get("de").getAsBoolean();

			JsonObject wrSupport = weaponRestrictions.get("support").getAsJsonObject();
			boolean grenade = wrSupport.get("grenade").getAsBoolean();
			boolean wp = wrSupport.get("wp").getAsBoolean();
			boolean stun = wrSupport.get("stun").getAsBoolean();
			boolean chaff = wrSupport.get("chaff").getAsBoolean();
			boolean smoke = wrSupport.get("smoke").getAsBoolean();
			boolean smoke_r = wrSupport.get("smoke_r").getAsBoolean();
			boolean smoke_g = wrSupport.get("smoke_g").getAsBoolean();
			boolean smoke_y = wrSupport.get("smoke_y").getAsBoolean();
			boolean eloc = wrSupport.get("eloc").getAsBoolean();
			boolean claymore = wrSupport.get("claymore").getAsBoolean();
			boolean sgmine = wrSupport.get("sgmine").getAsBoolean();
			boolean c4 = wrSupport.get("c4").getAsBoolean();
			boolean sgsatchel = wrSupport.get("sgsatchel").getAsBoolean();
			boolean magazine = wrSupport.get("magazine").getAsBoolean();

			JsonObject wrCustom = weaponRestrictions.get("custom").getAsJsonObject();
			boolean suppressor = wrCustom.get("suppressor").getAsBoolean();
			boolean gp30 = wrCustom.get("gp30").getAsBoolean();
			boolean xm320 = wrCustom.get("xm320").getAsBoolean();
			boolean masterkey = wrCustom.get("masterkey").getAsBoolean();
			boolean scope = wrCustom.get("scope").getAsBoolean();
			boolean sight = wrCustom.get("sight").getAsBoolean();
			boolean laser = wrCustom.get("laser").getAsBoolean();
			boolean lighthg = wrCustom.get("lighthg").getAsBoolean();
			boolean lightlg = wrCustom.get("lightlg").getAsBoolean();
			boolean grip = wrCustom.get("grip").getAsBoolean();

			JsonObject wrItems = weaponRestrictions.get("items").getAsJsonObject();
			boolean envg = wrItems.get("envg").getAsBoolean();
			boolean drum = wrItems.get("drum").getAsBoolean();

			JsonObject ruleSettings = details.get("ruleSettings").getAsJsonObject();
			JsonObject dm = ruleSettings.get("dm").getAsJsonObject();
			int dmTime = dm.get("time").getAsInt();
			int dmRounds = dm.get("rounds").getAsInt();
			int dmTickets = dm.get("tickets").getAsInt();

			JsonObject tdm = ruleSettings.get("tdm").getAsJsonObject();
			int tdmTime = tdm.get("time").getAsInt();
			int tdmRounds = tdm.get("rounds").getAsInt();
			int tdmTickets = tdm.get("tickets").getAsInt();

			JsonObject res = ruleSettings.get("res").getAsJsonObject();
			int resTime = res.get("time").getAsInt();
			int resRounds = res.get("rounds").getAsInt();

			JsonObject cap = ruleSettings.get("cap").getAsJsonObject();
			int capTime = cap.get("time").getAsInt();
			int capRounds = cap.get("rounds").getAsInt();
			boolean capExtraTime = cap.get("extraTime").getAsBoolean();

			JsonObject sne = ruleSettings.get("sne").getAsJsonObject();
			int sneTime = sne.get("time").getAsInt();
			int sneRounds = sne.get("rounds").getAsInt();
			int sneSnake = sne.get("snake").getAsInt();

			JsonObject base = ruleSettings.get("base").getAsJsonObject();
			int baseTime = base.get("time").getAsInt();
			int baseRounds = base.get("rounds").getAsInt();

			JsonObject bomb = ruleSettings.get("bomb").getAsJsonObject();
			int bombTime = bomb.get("time").getAsInt();
			int bombRounds = bomb.get("rounds").getAsInt();

			JsonObject tsne = ruleSettings.get("tsne").getAsJsonObject();
			int tsneTime = tsne.get("time").getAsInt();
			int tsneRounds = tsne.get("rounds").getAsInt();

			JsonObject sdm = ruleSettings.get("sdm").getAsJsonObject();
			int sdmTime = sdm.get("time").getAsInt();
			int sdmRounds = sdm.get("rounds").getAsInt();

			JsonObject intr = ruleSettings.get("int").getAsJsonObject();
			int intTime = intr.get("time").getAsInt();

			JsonObject scap = ruleSettings.get("scap").getAsJsonObject();
			int scapTime = scap.get("time").getAsInt();
			int scapRounds = scap.get("rounds").getAsInt();
			boolean scapExtraTime = scap.get("extraTime").getAsBoolean();

			JsonObject race = ruleSettings.get("race").getAsJsonObject();
			int raceTime = race.get("time").getAsInt();
			int raceRounds = race.get("rounds").getAsInt();
			boolean raceExtraTime = race.get("extraTime").getAsBoolean();

			int commonA = 0x4;
			if (idleKick > 0) {
				commonA += 1;
			}
			if (friendlyFire) {
				commonA += 8;
			}
			if (ghosts) {
				commonA += 16;
			}
			if (autoAim) {
				commonA += 32;
			}
			if (uniquesEnabled) {
				commonA += 128;
			}

			int commonB = 0;
			if (teamsSwitch) {
				commonB += 1;
			}
			if (autoAssign) {
				commonB += 2;
			}
			if (silentMode) {
				commonB += 4;
			}
			if (enemyNametags) {
				commonB += 8;
			}
			if (levelLimitEnabled) {
				commonB += 16;
			}
			if (voiceChat) {
				commonB += 64;
			}
			if (teamKillKick > 0) {
				commonB += 128;
			}

			int commonC = 0x20;

			int hostOptionsExtraTimeFlags = 0;
			if (nonStat) {
				hostOptionsExtraTimeFlags += 2;
			}
			if (!scapExtraTime) {
				hostOptionsExtraTimeFlags += 1;
			}
			if (!raceExtraTime) {
				hostOptionsExtraTimeFlags += 4;
			}

			byte[] wr = new byte[0x10];

			if (weaponRestrictionEnabled) {
				wr[0] += 0x1;
			}
			if (!knife) {
				wr[0] += 0x2;
			}
			if (!mk2) {
				wr[0] += 0x4;
			}
			if (!operator) {
				wr[0] += 0x8;
			}
			if (!mk23) {
				wr[0] += 0x10;
			}
			if (!gsr) {
				wr[0] += 0x80;
			}

			if (!de) {
				wr[1] += 0x1;
			}
			if (!g18) {
				wr[1] += 0x80;
			}

			if (!mp5) {
				wr[2] += 0x4;
			}
			if (!p90) {
				wr[2] += 0x10;
			}
			if (!patriot) {
				wr[2] += 0x40;
			}
			if (!vz) {
				wr[2] += 0x80;
			}

			if (!m4) {
				wr[3] += 0x1;
			}
			if (!ak) {
				wr[3] += 0x2;
			}
			if (!g3a3) {
				wr[3] += 0x4;
			}
			if (!mk17) {
				wr[3] += 0x40;
			}
			if (!xm8) {
				wr[3] += 0x80;
			}

			if (!m60) {
				wr[4] += 0x8;
			}
			if (!m870) {
				wr[4] += 0x20;
			}
			if (!saiga) {
				wr[4] += 0x40;
			}
			if (!vss) {
				wr[4] += 0x80;
			}

			if (!dsr) {
				wr[5] += 0x2;
			}
			if (!m14) {
				wr[5] += 0x4;
			}
			if (!mosin) {
				wr[5] += 0x8;
			}
			if (!svd) {
				wr[5] += 0x10;
			}

			if (!rpg) {
				wr[6] += 0x4;
			}
			if (!grenade) {
				wr[6] += 0x10;
			}
			if (!wp) {
				wr[6] += 0x20;
			}
			if (!stun) {
				wr[6] += 0x40;
			}
			if (!chaff) {
				wr[6] += 0x80;
			}

			if (!smoke) {
				wr[7] += 0x1;
			}
			if (!smoke_r) {
				wr[7] += 0x2;
			}
			if (!smoke_g) {
				wr[7] += 0x4;
			}
			if (!smoke_y) {
				wr[7] += 0x8;
			}
			if (!eloc) {
				wr[7] += 0x80;
			}

			if (!claymore) {
				wr[8] += 0x1;
			}
			if (!sgmine) {
				wr[8] += 0x2;
			}
			if (!c4) {
				wr[8] += 0x4;
			}
			if (!sgsatchel) {
				wr[8] += 0x8;
			}
			if (!magazine) {
				wr[8] += 0x20;
			}

			if (!shield) {
				wr[9] += 0x2;
			}
			if (!masterkey) {
				wr[9] += 0x4;
			}
			if (!xm320) {
				wr[9] += 0x8;
			}
			if (!gp30) {
				wr[9] += 0x10;
			}
			if (!suppressor) {
				wr[9] += 0x20;
			}

			if (!suppressor) {
				wr[10] += 0xe;
			}

			if (!scope) {
				wr[11] += 0x10;
			}
			if (!sight) {
				wr[11] += 0x20;
			}
			if (!lightlg) {
				wr[11] += 0x80;
			}

			if (!laser) {
				wr[12] += 0x1;
			}
			if (!lighthg) {
				wr[12] += 0x2;
			}
			if (!grip) {
				wr[12] += 0x4;
			}

			if (!drum) {
				wr[13] += 0x4;
			}

			if (!envg) {
				wr[14] += 0x40;
			}

			int lobbySubtype = 8;

			bo = ctx.alloc().directBuffer(0x36d);

			bo.writeInt(0).writeInt(gameId);
			Util.writeString(name, 0x10, bo);
			Util.writeString(comment, 0x80, bo);
			bo.writeZero(2).writeByte(lobbySubtype).writeInt(averageExperience).writeInt(hostScore).writeInt(hostVotes)
					.writeByte(0x1);

			for (JsonElement o : games) {
				JsonArray game = (JsonArray) o;
				int rule = game.get(0).getAsInt();
				int map = game.get(1).getAsInt();
				int flags = game.get(2).getAsInt();
				bo.writeByte(rule).writeByte(map).writeByte(flags);
			}

			Util.padTo(0xd5, bo);
			bo.writeZero(5).writeBytes(wr).writeByte(maxPlayers).writeByte(currentPlayers).writeInt(briefingTime)
					.writeZero(0x16).writeByte(stance).writeByte(levelLimitTolerance).writeInt(0x16).writeInt(sneTime)
					.writeInt(sneRounds).writeInt(capTime).writeInt(capRounds).writeInt(resTime).writeInt(resRounds)
					.writeInt(tdmTime).writeInt(tdmRounds).writeInt(tdmTickets).writeInt(dmTime).writeInt(dmTickets)
					.writeInt(baseTime).writeInt(baseRounds).writeInt(bombTime).writeInt(bombRounds).writeInt(tsneTime)
					.writeInt(tsneRounds);

			if (uniquesRandom) {
				bo.writeByte(0x80 + uniqueRed).writeByte(0x80 + uniqueBlue);
			} else {
				bo.writeByte(uniqueRed).writeByte(uniqueBlue);
			}

			bo.writeZero(7).writeByte(commonA).writeByte(commonB).writeZero(1).writeShort(idleKick)
					.writeShort(teamKillKick).writeInt(0x2e).writeBoolean(capExtraTime).writeByte(sneSnake)
					.writeByte(sdmTime).writeByte(sdmRounds).writeByte(intTime).writeByte(dmRounds).writeByte(scapTime)
					.writeByte(scapRounds).writeByte(raceTime).writeByte(raceRounds).writeZero(1)
					.writeByte(hostOptionsExtraTimeFlags).writeZero(4);

			for (JsonElement o : players) {
				JsonObject player = (JsonObject) o;
				int playerId = player.get("id").getAsInt();
				String playerName = player.get("name").getAsString();
				int playerPing = player.get("ping").getAsInt();
				int playerExp = player.get("exp").getAsInt();
				bo.writeInt(playerId);
				Util.writeString(playerName, 0x10, bo);
				bo.writeInt(playerPing).writeInt(playerExp);
			}

			Util.padTo(0x36d, bo);

			Packets.write(ctx, 0x4313, bo);
		} catch (Exception e) {
			logger.error("Exception while getting game info.", e);
			Packets.writeError(ctx, 0x4313, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void join(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int gameId = bi.readInt();
			String password = Util.readString(bi, 16);

			JsonObject data = new JsonObject();
			data.addProperty("session", NUsers.getSession(ctx));
			data.addProperty("game", gameId);
			data.addProperty("password", password);

			JsonObject response = Campbell.instance().getResponse("games", "getHostConnectionInfo", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while joining game: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4321, 2);
				return;
			}

			String publicIp = response.get("publicIp").getAsString();
			int publicPort = response.get("publicPort").getAsInt();
			String privateIp = response.get("privateIp").getAsString();
			int privatePort = response.get("privatePort").getAsInt();
			int rule = response.get("rule").getAsInt();
			int map = response.get("map").getAsInt();

			NUsers.setGameJoining(ctx, gameId);
			
			bo = ctx.alloc().directBuffer(0x2b);

			bo.writeInt(0);
			Util.writeString(publicIp, 16, bo);
			bo.writeShort(publicPort);
			Util.writeString(privateIp, 16, bo);
			bo.writeShort(privatePort);
			bo.writeByte(0).writeByte(rule).writeByte(map);

			Packets.write(ctx, 0x4321, bo);
		} catch (Exception e) {
			logger.error("Exception while getting joining game.", e);
			Packets.writeError(ctx, 0x4321, 1);
			Util.releaseBuffer(bo);
		}
	}
	
	public static void joinFailed(ChannelHandlerContext ctx, Packet in) {
		try {
			NUsers.setGameJoining(ctx, 0);

			Packets.write(ctx, 0x4323, 0);
		} catch (Exception e) {
			logger.error("Exception while handling join failed.", e);
			Packets.writeError(ctx, 0x4323, 1);
		}
	}

}
