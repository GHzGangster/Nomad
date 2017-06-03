package savemgo.nomad.helper;

import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.instance.NGames;
import savemgo.nomad.instance.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Hosts {

	private static final Logger logger = LogManager.getLogger(Hosts.class);

	public static void getSettings(ChannelHandlerContext ctx, int lobbySubtype) {
		ByteBuf bo = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("session", "");
			data.addProperty("type", lobbySubtype);

			JsonObject response = Campbell.instance().getResponse("hosts", "getHostSettings", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while getting host settings: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4305, 2);
				return;
			}

			JsonObject settings = response.get("settings").getAsJsonObject();

			String name = settings.get("name").getAsString();
			String password = settings.get("password").getAsString();
			int stance = settings.get("stance").getAsInt();
			String comment = settings.get("comment").getAsString();
			JsonArray games = settings.get("games").getAsJsonArray();

			JsonObject common = settings.get("common").getAsJsonObject();
			boolean dedicated = common.get("dedicated").getAsBoolean();
			int maxPlayers = common.get("maxPlayers").getAsInt();
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

			JsonObject ruleSettings = settings.get("ruleSettings").getAsJsonObject();
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

			int extraTimeFlags = 0;
			if (!scapExtraTime) {
				extraTimeFlags += 1;
			}
			if (!raceExtraTime) {
				extraTimeFlags += 4;
			}

			int hostOptions = 0;
			if (nonStat) {
				hostOptions += 2;
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

			bo = ctx.alloc().directBuffer(0x163);

			bo.writeInt(0);
			Util.writeString(name, 0x10, bo);
			Util.writeString(comment, 0x80, bo);

			if (!password.isEmpty()) {
				bo.writeByte(1);
				Util.writeString(password, 0x0f, bo);
				bo.writeZero(1);
			} else {
				bo.writeZero(0x11);
			}

			bo.writeBoolean(dedicated);

			for (JsonElement o : games) {
				JsonArray game = (JsonArray) o;
				int rule = game.get(0).getAsInt();
				int map = game.get(1).getAsInt();
				int flags = game.get(2).getAsInt();
				bo.writeByte(rule).writeByte(map).writeByte(flags);
			}

			Util.padTo(0xd8, bo);
			bo.writeBytes(wr, 0, 0x10).writeByte(maxPlayers).writeInt(briefingTime);
			bo.writeByte(0x2);
			Util.padTo(0xf9, bo);
			bo.writeByte(stance).writeByte(levelLimitTolerance).writeInt(levelLimitBase).writeInt(sneTime)
					.writeInt(sneRounds).writeInt(capTime).writeInt(capRounds).writeInt(resTime).writeInt(resRounds)
					.writeInt(tdmTime).writeInt(tdmRounds).writeInt(tdmTickets).writeInt(dmTime).writeInt(dmTickets)
					.writeInt(baseTime).writeInt(baseRounds).writeInt(bombTime).writeInt(bombRounds).writeInt(tsneTime)
					.writeInt(tsneRounds);

			if (uniquesRandom) {
				bo.writeByte(0x80 + uniqueRed).writeByte(0x80 + uniqueBlue);
			} else {
				bo.writeByte(uniqueRed).writeByte(uniqueBlue);
			}

			bo.writeByte(commonA).writeByte(commonB).writeByte(commonC).writeZero(1).writeByte(idleKick).writeZero(1)
					.writeByte(teamKillKick).writeBoolean(capExtraTime).writeByte(sneSnake).writeByte(sdmTime)
					.writeByte(sdmRounds).writeByte(intTime).writeByte(dmRounds).writeByte(scapTime)
					.writeByte(scapRounds).writeByte(raceTime).writeByte(raceRounds).writeZero(1)
					.writeByte(extraTimeFlags).writeByte(hostOptions).writeZero(10);

			Packets.write(ctx, 0x4305, bo);
		} catch (Exception e) {
			logger.error("Exception while getting host settings.", e);
			Packets.writeError(ctx, 0x4305, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void updateSettings(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();

			String name = Util.readString(bi, 0x10);
			String comment = Util.readString(bi, 0x80);
			boolean passwordEnabled = bi.readBoolean();
			String password = "";
			if (passwordEnabled) {
				password = Util.readString(bi, 0xf);
				bi.skipBytes(1);
			} else {
				bi.skipBytes(0x10);
			}
			boolean dedicated = bi.readBoolean();

			int lobbySubtype = bi.readByte();

			byte[] gamesBytes = new byte[0x2d];
			bi.readBytes(gamesBytes);

			bi.skipBytes(5);
			byte[] wr = new byte[0x10];
			bi.readBytes(wr);

			int maxPlayers = bi.readByte();
			int briefingTime = bi.readInt();
			bi.skipBytes(0xc);
			int stance = bi.readByte();
			int levelLimitTolerance = bi.readByte();
			int levelLimitBase = bi.readInt();
			int sneTime = bi.readInt();
			int sneRounds = bi.readInt();
			int capTime = bi.readInt();
			int capRounds = bi.readInt();
			int resTime = bi.readInt();
			int resRounds = bi.readInt();
			int tdmTime = bi.readInt();
			int tdmRounds = bi.readInt();
			int tdmTickets = bi.readInt();
			int dmTime = bi.readInt();
			int dmTickets = bi.readInt();
			int baseTime = bi.readInt();
			int baseRounds = bi.readInt();
			int bombTime = bi.readInt();
			int bombRounds = bi.readInt();
			int tsneTime = bi.readInt();
			int tsneRounds = bi.readInt();

			int uniqueRed = bi.readByte();
			int uniqueBlue = bi.readByte();

			int commonA = bi.readByte();
			int commonB = bi.readByte();
			int commonC = bi.readByte();
			bi.skipBytes(1);
			int idleKick = bi.readByte();
			bi.skipBytes(1);
			int teamKillKick = bi.readByte();
			boolean capExtraTime = bi.readBoolean();
			int sneSnake = bi.readByte();
			int sdmTime = bi.readByte();
			int sdmRounds = bi.readByte();
			int intTime = bi.readByte();
			int dmRounds = bi.readByte();
			int scapTime = bi.readByte();
			int scapRounds = bi.readByte();
			int raceTime = bi.readByte();
			int raceRounds = bi.readByte();
			bi.skipBytes(1);
			int extraTimeFlags = bi.readByte();
			int hostOptions = bi.readByte();

			JsonObject settings = new JsonObject();
			settings.addProperty("name", name);
			settings.addProperty("password", password);
			settings.addProperty("stance", stance);
			settings.addProperty("comment", comment);

			JsonArray games = new JsonArray();
			settings.add("games", games);
			for (int i = 0; i < 15; i++) {
				int rule = gamesBytes[i * 3];
				int map = gamesBytes[i * 3 + 1];
				int flags = gamesBytes[i * 3 + 2];
				if (rule == 0 && map == 0) {
					break;
				}
				JsonArray game = new JsonArray();
				games.add(game);
				game.add(rule);
				game.add(map);
				game.add(flags);
			}

			JsonObject common = new JsonObject();
			settings.add("common", common);
			common.addProperty("dedicated", dedicated);
			common.addProperty("maxPlayers", maxPlayers);
			common.addProperty("briefingTime", briefingTime);

			boolean nonStat = (hostOptions & 2) == 2;
			boolean friendlyFire = (commonA & 8) == 8;
			boolean autoAim = (commonA & 32) == 32;
			common.addProperty("nonStat", nonStat);
			common.addProperty("friendlyFire", friendlyFire);
			common.addProperty("autoAim", autoAim);

			boolean uniquesEnabled = (commonA & 128) == 128;
			boolean uniquesRandom = false;
			if ((uniqueRed & 0x80) == 0x80) {
				uniquesRandom = true;
				uniqueRed &= 0x7F;
				uniqueBlue &= 0x7F;
			}

			JsonObject uniques = new JsonObject();
			common.add("uniques", uniques);
			uniques.addProperty("enabled", uniquesEnabled);
			uniques.addProperty("random", uniquesRandom);
			uniques.addProperty("red", uniqueRed);
			uniques.addProperty("blue", uniqueBlue);

			boolean enemyNametags = (commonB & 8) == 8;
			boolean silentMode = (commonB & 4) == 4;
			boolean autoAssign = (commonB & 2) == 2;
			boolean teamsSwitch = (commonB & 1) == 1;
			boolean ghosts = (commonA & 16) == 16;
			common.addProperty("enemyNametags", enemyNametags);
			common.addProperty("silentMode", silentMode);
			common.addProperty("autoAssign", autoAssign);
			common.addProperty("teamsSwitch", teamsSwitch);
			common.addProperty("ghosts", ghosts);

			JsonObject levelLimit = new JsonObject();
			common.add("levelLimit", levelLimit);
			boolean levelLimitEnabled = (commonB & 16) == 16;
			levelLimit.addProperty("enabled", levelLimitEnabled);
			levelLimit.addProperty("base", levelLimitBase);
			levelLimit.addProperty("tolerance", levelLimitTolerance);

			boolean voiceChat = (commonB & 64) == 64;
			if ((commonB & 128) != 128) {
				teamKillKick = 0;
			}
			if ((commonA & 1) != 1) {
				idleKick = 0;
			}
			common.addProperty("voiceChat", voiceChat);
			common.addProperty("teamKillKick", teamKillKick);
			common.addProperty("idleKick", idleKick);

			JsonObject weaponRestrictions = new JsonObject();
			common.add("weaponRestrictions", weaponRestrictions);
			boolean weaponRestrictionsEnabled = (wr[0] & 1) == 1;
			weaponRestrictions.addProperty("enabled", weaponRestrictionsEnabled);

			JsonObject wrPrimary = new JsonObject();
			weaponRestrictions.add("primary", wrPrimary);
			boolean vz = (wr[2] & 0x80) == 0;
			boolean p90 = (wr[2] & 0x10) == 0;
			boolean mp5 = (wr[2] & 0x4) == 0;
			boolean patriot = (wr[2] & 0x40) == 0;
			boolean ak = (wr[3] & 0x2) == 0;
			boolean m4 = (wr[3] & 0x1) == 0;
			boolean mk17 = (wr[3] & 0x40) == 0;
			boolean xm8 = (wr[3] & 0x80) == 0;
			boolean g3a3 = (wr[3] & 0x4) == 0;
			boolean svd = (wr[5] & 0x10) == 0;
			boolean mosin = (wr[5] & 0x8) == 0;
			boolean m14 = (wr[5] & 0x4) == 0;
			boolean vss = (wr[4] & 0x80) == 0;
			boolean dsr = (wr[5] & 0x2) == 0;
			boolean m870 = (wr[4] & 0x20) == 0;
			boolean saiga = (wr[4] & 0x40) == 0;
			boolean m60 = (wr[4] & 0x8) == 0;
			boolean shield = (wr[9] & 0x2) == 0;
			boolean rpg = (wr[6] & 0x4) == 0;
			boolean knife = (wr[0] & 0x2) == 0;
			wrPrimary.addProperty("vz", vz);
			wrPrimary.addProperty("p90", p90);
			wrPrimary.addProperty("mp5", mp5);
			wrPrimary.addProperty("patriot", patriot);
			wrPrimary.addProperty("ak", ak);
			wrPrimary.addProperty("m4", m4);
			wrPrimary.addProperty("mk17", mk17);
			wrPrimary.addProperty("xm8", xm8);
			wrPrimary.addProperty("g3a3", g3a3);
			wrPrimary.addProperty("svd", svd);
			wrPrimary.addProperty("mosin", mosin);
			wrPrimary.addProperty("m14", m14);
			wrPrimary.addProperty("vss", vss);
			wrPrimary.addProperty("dsr", dsr);
			wrPrimary.addProperty("m870", m870);
			wrPrimary.addProperty("saiga", saiga);
			wrPrimary.addProperty("m60", m60);
			wrPrimary.addProperty("shield", shield);
			wrPrimary.addProperty("rpg", rpg);
			wrPrimary.addProperty("knife", knife);

			JsonObject wrSecondary = new JsonObject();
			weaponRestrictions.add("secondary", wrSecondary);
			boolean gsr = (wr[0] & 0x80) == 0;
			boolean mk2 = (wr[0] & 0x4) == 0;
			boolean operator = (wr[0] & 0x8) == 0;
			boolean g18 = (wr[1] & 0x80) == 0;
			boolean mk23 = (wr[0] & 0x10) == 0;
			boolean de = (wr[1] & 0x1) == 0;
			wrSecondary.addProperty("gsr", gsr);
			wrSecondary.addProperty("mk2", mk2);
			wrSecondary.addProperty("operator", operator);
			wrSecondary.addProperty("g18", g18);
			wrSecondary.addProperty("mk23", mk23);
			wrSecondary.addProperty("de", de);

			JsonObject wrSupport = new JsonObject();
			weaponRestrictions.add("support", wrSupport);
			boolean grenade = (wr[6] & 0x10) == 0;
			boolean wp = (wr[6] & 0x20) == 0;
			boolean stun = (wr[6] & 0x40) == 0;
			boolean chaff = (wr[6] & 0x80) == 0;
			boolean smoke = (wr[7] & 0x1) == 0;
			boolean smoke_r = (wr[7] & 0x2) == 0;
			boolean smoke_g = (wr[7] & 0x4) == 0;
			boolean smoke_y = (wr[7] & 0x8) == 0;
			boolean eloc = (wr[7] & 0x80) == 0;
			boolean claymore = (wr[8] & 0x1) == 0;
			boolean sgmine = (wr[8] & 0x2) == 0;
			boolean c4 = (wr[8] & 0x4) == 0;
			boolean sgsatchel = (wr[8] & 0x8) == 0;
			boolean magazine = (wr[8] & 0x20) == 0;
			wrSupport.addProperty("grenade", grenade);
			wrSupport.addProperty("wp", wp);
			wrSupport.addProperty("stun", stun);
			wrSupport.addProperty("chaff", chaff);
			wrSupport.addProperty("smoke", smoke);
			wrSupport.addProperty("smoke_r", smoke_r);
			wrSupport.addProperty("smoke_g", smoke_g);
			wrSupport.addProperty("smoke_y", smoke_y);
			wrSupport.addProperty("eloc", eloc);
			wrSupport.addProperty("claymore", claymore);
			wrSupport.addProperty("sgmine", sgmine);
			wrSupport.addProperty("c4", c4);
			wrSupport.addProperty("sgsatchel", sgsatchel);
			wrSupport.addProperty("magazine", magazine);

			JsonObject wrCustom = new JsonObject();
			weaponRestrictions.add("custom", wrCustom);
			boolean suppressor = (wr[9] & 0x20) == 0;
			boolean gp30 = (wr[9] & 0x10) == 0;
			boolean xm320 = (wr[9] & 0x8) == 0;
			boolean masterkey = (wr[9] & 0x4) == 0;
			boolean scope = (wr[11] & 0x10) == 0;
			boolean sight = (wr[11] & 0x20) == 0;
			boolean laser = (wr[12] & 0x1) == 0;
			boolean lighthg = (wr[12] & 0x2) == 0;
			boolean lightlg = (wr[11] & 0x80) == 0;
			boolean grip = (wr[12] & 0x4) == 0;
			wrCustom.addProperty("suppressor", suppressor);
			wrCustom.addProperty("gp30", gp30);
			wrCustom.addProperty("xm320", xm320);
			wrCustom.addProperty("masterkey", masterkey);
			wrCustom.addProperty("scope", scope);
			wrCustom.addProperty("sight", sight);
			wrCustom.addProperty("laser", laser);
			wrCustom.addProperty("lighthg", lighthg);
			wrCustom.addProperty("lightlg", lightlg);
			wrCustom.addProperty("grip", grip);

			JsonObject wrItems = new JsonObject();
			weaponRestrictions.add("items", wrItems);
			boolean envg = (wr[14] & 0x40) == 0;
			boolean drum = (wr[13] & 0x4) == 0;
			wrItems.addProperty("envg", envg);
			wrItems.addProperty("drum", drum);

			JsonObject ruleSettings = new JsonObject();
			settings.add("ruleSettings", ruleSettings);

			JsonObject dm = new JsonObject();
			ruleSettings.add("dm", dm);
			dm.addProperty("time", dmTime);
			dm.addProperty("rounds", dmRounds);
			dm.addProperty("tickets", dmTickets);

			JsonObject tdm = new JsonObject();
			ruleSettings.add("tdm", tdm);
			tdm.addProperty("time", tdmTime);
			tdm.addProperty("rounds", tdmRounds);
			tdm.addProperty("tickets", tdmTickets);

			JsonObject res = new JsonObject();
			ruleSettings.add("res", res);
			res.addProperty("time", resTime);
			res.addProperty("rounds", resRounds);

			JsonObject cap = new JsonObject();
			ruleSettings.add("cap", cap);
			cap.addProperty("time", capTime);
			cap.addProperty("rounds", capRounds);
			cap.addProperty("extraTime", capExtraTime);

			JsonObject sne = new JsonObject();
			ruleSettings.add("sne", sne);
			sne.addProperty("time", sneTime);
			sne.addProperty("rounds", sneRounds);
			sne.addProperty("snake", sneSnake);

			JsonObject base = new JsonObject();
			ruleSettings.add("base", base);
			base.addProperty("time", baseTime);
			base.addProperty("rounds", baseRounds);

			JsonObject bomb = new JsonObject();
			ruleSettings.add("bomb", bomb);
			bomb.addProperty("time", bombTime);
			bomb.addProperty("rounds", bombRounds);

			JsonObject tsne = new JsonObject();
			ruleSettings.add("tsne", tsne);
			tsne.addProperty("time", tsneTime);
			tsne.addProperty("rounds", tsneRounds);

			JsonObject sdm = new JsonObject();
			ruleSettings.add("sdm", sdm);
			sdm.addProperty("time", sdmTime);
			sdm.addProperty("rounds", sdmRounds);

			JsonObject intr = new JsonObject();
			ruleSettings.add("int", intr);
			intr.addProperty("time", intTime);

			JsonObject scap = new JsonObject();
			ruleSettings.add("scap", scap);
			scap.addProperty("time", scapTime);
			scap.addProperty("rounds", scapRounds);
			boolean scapExtraTime = (extraTimeFlags & 1) == 0;
			scap.addProperty("extraTime", scapExtraTime);

			JsonObject race = new JsonObject();
			ruleSettings.add("race", race);
			race.addProperty("time", raceTime);
			race.addProperty("rounds", raceRounds);
			boolean raceExtraTime = (extraTimeFlags & 4) == 0;
			race.addProperty("extraTime", raceExtraTime);

			JsonObject data = new JsonObject();
			data.addProperty("session", "");
			data.addProperty("type", lobbySubtype);
			data.add("settings", settings);

			JsonObject response = Campbell.instance().getResponse("hosts", "updateHostSettings", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while updating host settings: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4311, 2);
				return;
			}

			Packets.write(ctx, 0x4311, 0);
		} catch (Exception e) {
			logger.error("Exception while updating host settings.", e);
			Packets.writeError(ctx, 0x4311, 1);
		}
	}

	public static void createGame(ChannelHandlerContext ctx, int lobbyId) {
		ByteBuf bo = null;
		try {
			int chara = 0;

			JsonObject data = new JsonObject();
			data.addProperty("session", "");
			data.addProperty("lobby", lobbyId);

			JsonObject response = Campbell.instance().getResponse("hosts", "createGame", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while creating game: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4317, 2);
				return;
			}

			int id = response.get("game").getAsInt();

			HashMap<String, Object> settings = new HashMap<>();
			settings.put("name", "Game " + id);

//			if (NGames.initialize(id, chara, settings) == null) {
//				logger.error("Failed to initialize game instance.");
//				Packets.writeError(ctx, 0x4317, 3);
//				return;
//			}

//			NUsers.setGame(ctx, id);

			bo = ctx.alloc().directBuffer(0x8);

			bo.writeInt(0).writeInt(id);

			Packets.write(ctx, 0x4317, bo);
		} catch (Exception e) {
			logger.error("Exception while creating game.", e);
			Packets.writeError(ctx, 0x4317, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void playerChangedTeam(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int playerId = bi.readInt();
			int team = bi.readByte();

			bo = ctx.alloc().directBuffer(0x8);

			bo.writeInt(0).writeInt(playerId);

			Packets.write(ctx, 0x4345, bo);
		} catch (Exception e) {
			logger.error("Exception while handling team join.", e);
			Packets.writeError(ctx, 0x4345, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void playerConnected(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			int gameId = 0;

			int chara = 0;
			int host = 0;
			if (chara != host) {
				logger.error("Error while handling player connected: Not the host.");
				Packets.writeError(ctx, 0x4341, 3);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetId = bi.readInt();

			ConcurrentMap<String, Object> target = null;
			int gameJoining = (Integer) target.get("gameJoining");
			
			if (gameJoining != gameId) {
				
			}
			
			bo = ctx.alloc().directBuffer(0x8);

			bo.writeInt(0).writeInt(targetId);

			Packets.write(ctx, 0x4341, bo);
		} catch (Exception e) {
			logger.error("Exception while handling player connected.", e);
			Packets.writeError(ctx, 0x4341, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void playerDisconnected(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int playerId = bi.readInt();

			bo = ctx.alloc().directBuffer(0x8);

			bo.writeInt(0).writeInt(playerId);

			Packets.write(ctx, 0x4343, bo);
		} catch (Exception e) {
			logger.error("Exception while handling player disconnected.", e);
			Packets.writeError(ctx, 0x4343, 1);
			Util.releaseBuffer(bo);
		}
	}

	public static void setGame(ChannelHandlerContext ctx, Packet in) {
		try {
			ByteBuf bi = in.getPayload();
			int game = bi.readByte();

			JsonObject data = new JsonObject();
			data.addProperty("session", "");
			data.addProperty("game", game);

			JsonObject response = Campbell.instance().getResponse("hosts", "setGame", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while setting game: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4393, 2);
				return;
			}

			Packets.write(ctx, 0x4393, 0);
		} catch (Exception e) {
			logger.error("Exception while setting game.", e);
			// Fail silently
		}
	}

	public static void endGame(ChannelHandlerContext ctx) {
		try {
			int gameId = 0;

			int chara = 0;
			int host = 0;
			if (chara != host) {
				logger.error("Error while ending game: Not the host.");
				Packets.writeError(ctx, 0x4381, 3);
				return;
			}

			JsonObject data = new JsonObject();
			data.addProperty("session", "");
			data.addProperty("game", gameId);

			JsonObject response = Campbell.instance().getResponse("hosts", "endGame", data);
			if (!Campbell.checkResult(response)) {
				logger.error("Error while ending game: " + Campbell.getResult(response));
				Packets.writeError(ctx, 0x4381, 2);
				return;
			}

//			NUsers.setGame(ctx, 0);
//			NGames.finalize(gameId);

			Packets.write(ctx, 0x4381, 0);
		} catch (Exception e) {
			logger.error("Exception while ending game.", e);
			Packets.writeError(ctx, 0x4381, 1);
		}
	}

}
