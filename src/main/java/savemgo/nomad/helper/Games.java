package savemgo.nomad.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.ConnectionInfo;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NGames;
import savemgo.nomad.instances.NUsers;
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

	public static void getList(ChannelHandlerContext ctx, Lobby lobby, int command) {
		AtomicReference<ByteBuf[]> payloads = new AtomicReference<>();
		try {
			Collection<Game> games = NGames.getGames();
			ArrayList<Game> gamez = new ArrayList<>();
			for (Game game : games) {
				if (game.getLobbyId() == lobby.getId()) {
					gamez.add(game);
				}
			}

			Packets.handleMutliElementPayload(ctx, gamez.size(), 18, 0x37, payloads, (i, bo) -> {
				Game game = gamez.get(i);

				String jsonGames = game.getGames();
				String jsonCommon = game.getCommon();
				String jsonRules = game.getRules();

				List<Player> players = game.getPlayers();

				JsonObject common = Util.jsonDecode(jsonCommon);
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

				JsonArray jGames = Util.jsonDecodeArray(jsonGames);
				JsonArray jGame = jGames.get(game.getCurrentGame()).getAsJsonArray();
				int rule = jGame.get(0).getAsInt();
				int map = jGame.get(1).getAsInt();

				int hostScore = game.getHost().getHostScore();
				int hostVotes = game.getHost().getHostVotes();

				int averageExperience = 0;
				int numPlayers = players.size();
				for (Player player : players) {
					averageExperience += player.getCharacter().getExp();
				}
				if (numPlayers > 0) {
					averageExperience /= numPlayers;
				}

				int hostOptions = 0;
				hostOptions |= game.getPassword() != null ? 0b1 : 0;
				hostOptions |= dedicated ? 0b10 : 0;

				int commonA = 0b100;
				commonA |= idleKick > 0 ? 0b1 : 0;
				commonA |= friendlyFire ? 0b1000 : 0;
				commonA |= ghosts ? 0b10000 : 0;
				commonA |= autoAim ? 0b100000 : 0;
				commonA |= uniquesEnabled ? 0b10000000 : 0;

				int commonB = 0;
				commonB |= teamsSwitch ? 0b1 : 0;
				commonB |= autoAssign ? 0b10 : 0;
				commonB |= silentMode ? 0b100 : 0;
				commonB |= enemyNametags ? 0b1000 : 0;
				commonB |= levelLimitEnabled ? 0b10000 : 0;
				commonB |= voiceChat ? 0b1000000 : 0;
				commonB |= teamKillKick > 0 ? 0b10000000 : 0;

				int friendBlock = 0;

				int unknown = 0x8;

				bo.writeInt(game.getId());
				Util.writeString(game.getName(), 16, bo);
				bo.writeByte(hostOptions).writeByte(unknown).writeByte(rule).writeByte(map).writeZero(1)
						.writeByte(game.getMaxPlayers()).writeByte(game.getStance()).writeByte(commonA)
						.writeByte(commonB).writeByte(numPlayers).writeInt(game.getPing()).writeByte(friendBlock)
						.writeByte(levelLimitTolerance).writeInt(levelLimitBase).writeInt(averageExperience)
						.writeInt(hostScore).writeInt(hostVotes).writeZero(2).writeByte(0x63);
			});

			Packets.write(ctx, command, 0);
			Packets.write(ctx, command + 1, payloads);
			Packets.write(ctx, command + 2, 0);
		} catch (Exception e) {
			logger.error("Exception while getting game list.", e);
			Util.releaseBuffers(payloads);
			Packets.writeError(ctx, command, 1);
		}
	}

	public static void getDetails(ChannelHandlerContext ctx, Packet in, Lobby lobby) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int gameId = bi.readInt();

			Game game = NGames.get(gameId);

			String jsonGames = game.getGames();
			String jsonCommon = game.getCommon();
			String jsonRules = game.getRules();

			List<Player> players = game.getPlayers();

			JsonObject common = Util.jsonDecode(jsonCommon);
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

			JsonArray jGames = Util.jsonDecodeArray(jsonGames);

			int averageExperience = 0;
			int numPlayers = players.size();
			for (Player player : players) {
				averageExperience += player.getCharacter().getExp();
			}
			if (numPlayers > 0) {
				averageExperience /= numPlayers;
			}

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

			JsonObject ruleSettings = Util.jsonDecode(jsonRules);
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

			int commonA = 0b100;
			commonA |= idleKick > 0 ? 0b1 : 0;
			commonA |= friendlyFire ? 0b1000 : 0;
			commonA |= ghosts ? 0b10000 : 0;
			commonA |= autoAim ? 0b100000 : 0;
			commonA |= uniquesEnabled ? 0b10000000 : 0;

			int commonB = 0;
			commonB |= teamsSwitch ? 0b1 : 0;
			commonB |= autoAssign ? 0b10 : 0;
			commonB |= silentMode ? 0b100 : 0;
			commonB |= enemyNametags ? 0b1000 : 0;
			commonB |= levelLimitEnabled ? 0b10000 : 0;
			commonB |= voiceChat ? 0b1000000 : 0;
			commonB |= teamKillKick > 0 ? 0b10000000 : 0;

			int commonC = 0x20;

			int hostOptionsExtraTimeFlags = 0;
			hostOptionsExtraTimeFlags |= !scapExtraTime ? 0b1 : 0;
			hostOptionsExtraTimeFlags |= nonStat ? 0b10 : 0;
			hostOptionsExtraTimeFlags |= !raceExtraTime ? 0b100 : 0;

			byte[] wr = new byte[0x10];
			wr[0] |= weaponRestrictionEnabled ? 0b1 : 0;
			wr[0] |= !knife ? 0b10 : 0;
			wr[0] |= !mk2 ? 0b100 : 0;
			wr[0] |= !operator ? 0b100 : 0;
			wr[0] |= !mk23 ? 0b10000 : 0;
			wr[0] |= !gsr ? 0b10000000 : 0;

			wr[1] |= !de ? 0b1 : 0;
			wr[1] |= !g18 ? 0b10000000 : 0;

			wr[2] |= !mp5 ? 0b100 : 0;
			wr[2] |= !p90 ? 0b10000 : 0;
			wr[2] |= !patriot ? 0b1000000 : 0;
			wr[2] |= !vz ? 0b10000000 : 0;

			wr[3] |= !m4 ? 0b1 : 0;
			wr[3] |= !ak ? 0b10 : 0;
			wr[3] |= !g3a3 ? 0b100 : 0;
			wr[3] |= !mk17 ? 0b1000000 : 0;
			wr[3] |= !xm8 ? 0b10000000 : 0;

			wr[4] |= !m60 ? 0b1000 : 0;
			wr[4] |= !m870 ? 0b100000 : 0;
			wr[4] |= !saiga ? 0b1000000 : 0;
			wr[4] |= !vss ? 0b10000000 : 0;

			wr[5] |= !dsr ? 0b10 : 0;
			wr[5] |= !m14 ? 0b100 : 0;
			wr[5] |= !mosin ? 0b1000 : 0;
			wr[5] |= !svd ? 0b10000 : 0;

			wr[6] |= !rpg ? 0b100 : 0;
			wr[6] |= !grenade ? 0b10000 : 0;
			wr[6] |= !wp ? 0b100000 : 0;
			wr[6] |= !stun ? 0b1000000 : 0;
			wr[6] |= !chaff ? 0b10000000 : 0;

			wr[7] |= !smoke ? 0b1 : 0;
			wr[7] |= !smoke_r ? 0b10 : 0;
			wr[7] |= !smoke_g ? 0b100 : 0;
			wr[7] |= !smoke_y ? 0b1000 : 0;
			wr[7] |= !eloc ? 0b10000000 : 0;

			wr[8] |= !claymore ? 0b1 : 0;
			wr[8] |= !sgmine ? 0b10 : 0;
			wr[8] |= !c4 ? 0b100 : 0;
			wr[8] |= !sgsatchel ? 0b1000 : 0;
			wr[8] |= !magazine ? 0b100000 : 0;

			wr[9] |= !shield ? 0b10 : 0;
			wr[9] |= !masterkey ? 0b100 : 0;
			wr[9] |= !xm320 ? 0b1000 : 0;
			wr[9] |= !gp30 ? 0b10000 : 0;
			wr[9] |= !suppressor ? 0b100000 : 0;

			wr[10] |= !suppressor ? 0b1110 : 0;

			wr[11] |= !scope ? 0b10000 : 0;
			wr[11] |= !sight ? 0b100000 : 0;
			wr[11] |= !lightlg ? 0b10000000 : 0;

			wr[12] |= !laser ? 0b1 : 0;
			wr[12] |= !lighthg ? 0b10 : 0;
			wr[12] |= !grip ? 0b100 : 0;

			wr[13] |= !drum ? 0b100 : 0;

			wr[14] |= !envg ? 0b1000000 : 0;

			int lobbySubtype = lobby.getSubtype();

			bo = ctx.alloc().directBuffer(0x36d);

			bo.writeInt(0).writeInt(gameId);
			Util.writeString(game.getName(), 0x10, bo);
			Util.writeString(game.getComment(), 0x80, bo);
			bo.writeZero(2).writeByte(lobbySubtype).writeInt(averageExperience).writeInt(game.getHost().getHostScore())
					.writeInt(game.getHost().getHostVotes()).writeByte(0x1);

			for (JsonElement o : jGames) {
				JsonArray game0 = (JsonArray) o;
				int rule0 = game0.get(0).getAsInt();
				int map0 = game0.get(1).getAsInt();
				int flags0 = game0.get(2).getAsInt();
				bo.writeByte(rule0).writeByte(map0).writeByte(flags0);
			}

			Util.padTo(0xd5, bo);
			bo.writeZero(5).writeBytes(wr).writeByte(game.getMaxPlayers()).writeByte(numPlayers).writeInt(briefingTime)
					.writeZero(0x16).writeByte(game.getStance()).writeByte(levelLimitTolerance).writeInt(0x16)
					.writeInt(sneTime).writeInt(sneRounds).writeInt(capTime).writeInt(capRounds).writeInt(resTime)
					.writeInt(resRounds).writeInt(tdmTime).writeInt(tdmRounds).writeInt(tdmTickets).writeInt(dmTime)
					.writeInt(dmTickets).writeInt(baseTime).writeInt(baseRounds).writeInt(bombTime).writeInt(bombRounds)
					.writeInt(tsneTime).writeInt(tsneRounds);

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

			for (Player player : players) {
				bo.writeInt(player.getCharacterId());
				Util.writeString(player.getCharacter().getName(), 0x10, bo);
				bo.writeInt(player.getPing()).writeInt(player.getCharacter().getExp());
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
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while joining game: No user.");
				Packets.writeError(ctx, 0x4321, 2);
				return;
			}

			Character character = user.getCurrentCharacter();

			ByteBuf bi = in.getPayload();
			int gameId = bi.readInt();
			String password = Util.readString(bi, 16);

			Game game = NGames.get(gameId);
			if (game == null) {
				logger.error("Error while joining game: No game.");
				Packets.writeError(ctx, 0x4321, 3);
				return;
			}

			List<ConnectionInfo> connectionInfoList = game.getHost().getConnectionInfo();
			if (connectionInfoList.size() <= 0) {
				logger.error("Error while joining game: No connection info.");
				Packets.writeError(ctx, 0x4321, 4);
				return;
			}
			ConnectionInfo connectionInfo = connectionInfoList.get(0);
			
			String jsonGames = game.getGames();

			JsonArray games = Util.jsonDecodeArray(jsonGames);
			JsonArray mapRule = games.get(game.getCurrentGame()).getAsJsonArray();

			int rule = mapRule.get(0).getAsInt();
			int map = mapRule.get(1).getAsInt();

			character.setGameJoining(game.getId());
			logger.debug("Character game joining: {}", character.getGameJoining());

			bo = ctx.alloc().directBuffer(0x2b);

			bo.writeInt(0);
			Util.writeString(connectionInfo.getPublicIp(), 16, bo);
			bo.writeShort(connectionInfo.getPublicPort());
			Util.writeString(connectionInfo.getPrivateIp(), 16, bo);
			bo.writeShort(connectionInfo.getPrivatePort());
			bo.writeByte(0).writeByte(rule).writeByte(map);

			Packets.write(ctx, 0x4321, bo);
		} catch (Exception e) {
			logger.error("Exception while joining game.", e);
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4321, 1);
		}
	}

	public static void joinFailed(ChannelHandlerContext ctx, Packet in) {
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while handling join failed: No user.");
				Packets.writeError(ctx, 0x4323, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			character.setGameJoining(null);

			Packets.write(ctx, 0x4323, 0);
		} catch (Exception e) {
			logger.error("Exception while handling join failed.", e);
			Packets.writeError(ctx, 0x4323, 1);
		}
	}

}
