package savemgo.nomad.helper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import savemgo.nomad.db.DB;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.CharacterHostSettings;
import savemgo.nomad.entity.EventCreateGame;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NGames;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.plugin.PluginHandler;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Hosts {

	private static final Logger logger = LogManager.getLogger();

	private static final String SETTINGS_DEFAULT = "{\"name\":\"{CHARACTER_NAME}\",\"password\":null,\"stance\":0,\"comment\":\"Good luck.\",\"games\":[],\"common\":{\"dedicated\":false,\"maxPlayers\":16,\"briefingTime\":2,\"nonStat\":false,\"friendlyFire\":false,\"autoAim\":true,\"uniques\":{\"enabled\":false,\"random\":false,\"red\":0,\"blue\":2},\"enemyNametags\":true,\"silentMode\":false,\"autoAssign\":true,\"teamsSwitch\":true,\"ghosts\":false,\"levelLimit\":{\"enabled\":false,\"base\":{CHARACTER_EXP},\"tolerance\":0},\"voiceChat\":true,\"teamKillKick\":3,\"idleKick\":5,\"weaponRestrictions\":{\"enabled\":false,\"primary\":{\"vz\":true,\"p90\":true,\"mp5\":true,\"patriot\":true,\"ak\":true,\"m4\":true,\"mk17\":true,\"xm8\":true,\"g3a3\":true,\"svd\":true,\"mosin\":true,\"m14\":true,\"vss\":true,\"dsr\":true,\"m870\":true,\"saiga\":true,\"m60\":true,\"shield\":true,\"rpg\":true,\"knife\":true},\"secondary\":{\"gsr\":true,\"mk2\":true,\"operator\":true,\"g18\":true,\"mk23\":true,\"de\":true},\"support\":{\"grenade\":true,\"wp\":true,\"stun\":true,\"chaff\":true,\"smoke\":true,\"smoke_r\":true,\"smoke_g\":true,\"smoke_y\":true,\"eloc\":true,\"claymore\":true,\"sgmine\":true,\"c4\":true,\"sgsatchel\":true,\"magazine\":true},\"custom\":{\"suppressor\":true,\"gp30\":true,\"xm320\":true,\"masterkey\":true,\"scope\":true,\"sight\":true,\"laser\":true,\"lighthg\":true,\"lightlg\":true,\"grip\":true},\"items\":{\"envg\":true,\"drum\":true}}},\"ruleSettings\":{\"dm\":{\"time\":5,\"rounds\":1,\"tickets\":30},\"tdm\":{\"time\":5,\"rounds\":2,\"tickets\":51},\"res\":{\"time\":7,\"rounds\":2},\"cap\":{\"time\":4,\"rounds\":2,\"extraTime\":false},\"sne\":{\"time\":7,\"rounds\":2,\"snake\":3},\"base\":{\"time\":5,\"rounds\":2},\"bomb\":{\"time\":7,\"rounds\":2},\"tsne\":{\"time\":10,\"rounds\":2},\"sdm\":{\"time\":3,\"rounds\":2},\"int\":{\"time\":20},\"scap\":{\"time\":5,\"rounds\":2,\"extraTime\":true},\"race\":{\"time\":5,\"rounds\":2,\"extraTime\":true}}}";

	public static void getSettings(ChannelHandlerContext ctx, Lobby lobby) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while getting host settings: No User.");
				Packets.writeError(ctx, 0x4305, 2);
				return;
			}

			Character character = user.getCurrentCharacter();
			List<CharacterHostSettings> settingsList = character.getHostSettings();
			if (settingsList == null) {
				settingsList = new ArrayList<>();
				character.setHostSettings(settingsList);
			}

			CharacterHostSettings hostSettings = settingsList.stream().filter((e) -> e.getType() == lobby.getSubtype())
					.findFirst().orElse(null);
			if (hostSettings == null) {
				int exp = 0;
				if (user.getMainCharacterId() != null && character.getId().equals(user.getMainCharacterId())) {
					exp = user.getMainExp();
				} else {
					exp = user.getAltExp();
				}

				String settingsStr = SETTINGS_DEFAULT.replaceFirst(Pattern.quote("{CHARACTER_NAME}"),
						character.getName());
				settingsStr = settingsStr.replaceFirst(Pattern.quote("{CHARACTER_EXP}"), exp + "");

				hostSettings = new CharacterHostSettings();
				hostSettings.setCharacter(character);
				hostSettings.setType(lobby.getSubtype());
				hostSettings.setSettings(settingsStr);

				settingsList.add(hostSettings);
			}

			JsonObject settings = Util.jsonDecode(hostSettings.getSettings());

			String name = settings.get("name").getAsString();
			String password = settings.get("password") != null && !settings.get("password").isJsonNull()
					? settings.get("password").getAsString() : null;
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

			int extraTimeFlags = 0;
			extraTimeFlags |= !scapExtraTime ? 0b1 : 0;
			extraTimeFlags |= !raceExtraTime ? 0b100 : 0;

			int hostOptions = 0;
			hostOptions |= nonStat ? 0b10 : 0;

			byte[] wr = new byte[0x10];
			wr[0] |= weaponRestrictionEnabled ? 0b1 : 0;
			wr[0] |= !knife ? 0b10 : 0;
			wr[0] |= !mk2 ? 0b100 : 0;
			wr[0] |= !operator ? 0b1000 : 0;
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

			bo = ctx.alloc().directBuffer(0x163);

			bo.writeInt(0);
			Util.writeString(name, 0x10, bo);
			Util.writeString(comment, 0x80, bo);

			if (password != null) {
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
			Util.releaseBuffer(bo);
			Packets.writeError(ctx, 0x4305, 1);
		}
	}

	public static void checkSettings(ChannelHandlerContext ctx, Packet in, Lobby lobby) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while updating host settings: No User.");
				Packets.write(ctx, 0x4311, Error.INVALID_SESSION);
				return;
			}

			Character character = user.getCurrentCharacter();
			List<CharacterHostSettings> settingsList = character.getHostSettings();
			if (settingsList == null) {
				settingsList = new ArrayList<>();
				character.setHostSettings(settingsList);
			}

			boolean saveSettings = true;

			ByteBuf bi = in.getPayload();

			String name = Util.readString(bi, 0x10, CharsetUtil.UTF_8);
			String comment = Util.readString(bi, 0x80, CharsetUtil.UTF_8);
			boolean passwordEnabled = bi.readBoolean();
			String password = null;
			if (passwordEnabled) {
				password = Util.readString(bi, 0xf);
				bi.skipBytes(1);
			} else {
				bi.skipBytes(0x10);
			}
			boolean dedicated = bi.readBoolean();

			int lobbySubtype2 = bi.readByte();

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
				if (rule == 11) {
					// Clan Room
					saveSettings = false;
				}
				JsonArray game = new JsonArray();
				games.add(game);
				game.add(rule);
				game.add(map);
				game.add(flags);
			}

			if (games.size() <= 0) {
				logger.error("Error while updating host settings: No Games.");
				Packets.writeError(ctx, 0x4311, 2);
				return;
			}

			JsonObject common = new JsonObject();
			settings.add("common", common);
			common.addProperty("dedicated", dedicated);
			common.addProperty("maxPlayers", maxPlayers);
			common.addProperty("briefingTime", briefingTime);

			boolean nonStat = (hostOptions & 0b10) == 0b10;
			boolean friendlyFire = (commonA & 0b1000) == 0b1000;
			boolean autoAim = (commonA & 0b100000) == 0b100000;
			common.addProperty("nonStat", nonStat);
			common.addProperty("friendlyFire", friendlyFire);
			common.addProperty("autoAim", autoAim);

			boolean uniquesEnabled = (commonA & 0b10000000) == 0b10000000;
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

			boolean enemyNametags = (commonB & 0b1000) == 0b1000;
			boolean silentMode = (commonB & 0b100) == 0b100;
			boolean autoAssign = (commonB & 0b10) == 0b10;
			boolean teamsSwitch = (commonB & 0b1) == 0b1;
			boolean ghosts = (commonA & 0b10000) == 0b10000;
			common.addProperty("enemyNametags", enemyNametags);
			common.addProperty("silentMode", silentMode);
			common.addProperty("autoAssign", autoAssign);
			common.addProperty("teamsSwitch", teamsSwitch);
			common.addProperty("ghosts", ghosts);

			JsonObject levelLimit = new JsonObject();
			common.add("levelLimit", levelLimit);
			boolean levelLimitEnabled = (commonB & 0b10000) == 0b10000;
			levelLimit.addProperty("enabled", levelLimitEnabled);
			levelLimit.addProperty("base", levelLimitBase);
			levelLimit.addProperty("tolerance", levelLimitTolerance);

			boolean voiceChat = (commonB & 0b1000000) == 0b1000000;
			if ((commonB & 0b10000000) != 0b10000000) {
				teamKillKick = 0;
			}
			if ((commonA & 0b1) != 0b1) {
				idleKick = 0;
			}
			common.addProperty("voiceChat", voiceChat);
			common.addProperty("teamKillKick", teamKillKick);
			common.addProperty("idleKick", idleKick);

			JsonObject weaponRestrictions = new JsonObject();
			common.add("weaponRestrictions", weaponRestrictions);
			boolean weaponRestrictionsEnabled = (wr[0] & 0b1) == 0b1;
			weaponRestrictions.addProperty("enabled", weaponRestrictionsEnabled);

			JsonObject wrPrimary = new JsonObject();
			weaponRestrictions.add("primary", wrPrimary);
			boolean vz = (wr[2] & 0b10000000) == 0;
			boolean p90 = (wr[2] & 0b10000) == 0;
			boolean mp5 = (wr[2] & 0b100) == 0;
			boolean patriot = (wr[2] & 0b1000000) == 0;
			boolean ak = (wr[3] & 0b10) == 0;
			boolean m4 = (wr[3] & 0b1) == 0;
			boolean mk17 = (wr[3] & 0b1000000) == 0;
			boolean xm8 = (wr[3] & 0b10000000) == 0;
			boolean g3a3 = (wr[3] & 0b100) == 0;
			boolean svd = (wr[5] & 0b10000) == 0;
			boolean mosin = (wr[5] & 0b1000) == 0;
			boolean m14 = (wr[5] & 0b100) == 0;
			boolean vss = (wr[4] & 0b10000000) == 0;
			boolean dsr = (wr[5] & 0b10) == 0;
			boolean m870 = (wr[4] & 0b100000) == 0;
			boolean saiga = (wr[4] & 0b1000000) == 0;
			boolean m60 = (wr[4] & 0b1000) == 0;
			boolean shield = (wr[9] & 0b10) == 0;
			boolean rpg = (wr[6] & 0b100) == 0;
			boolean knife = (wr[0] & 0b10) == 0;
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
			boolean gsr = (wr[0] & 0b10000000) == 0;
			boolean mk2 = (wr[0] & 0b100) == 0;
			boolean operator = (wr[0] & 0b1000) == 0;
			boolean g18 = (wr[1] & 0b10000000) == 0;
			boolean mk23 = (wr[0] & 0b10000) == 0;
			boolean de = (wr[1] & 0b1) == 0;
			wrSecondary.addProperty("gsr", gsr);
			wrSecondary.addProperty("mk2", mk2);
			wrSecondary.addProperty("operator", operator);
			wrSecondary.addProperty("g18", g18);
			wrSecondary.addProperty("mk23", mk23);
			wrSecondary.addProperty("de", de);

			JsonObject wrSupport = new JsonObject();
			weaponRestrictions.add("support", wrSupport);
			boolean grenade = (wr[6] & 0b10000) == 0;
			boolean wp = (wr[6] & 0b100000) == 0;
			boolean stun = (wr[6] & 0b1000000) == 0;
			boolean chaff = (wr[6] & 0b10000000) == 0;
			boolean smoke = (wr[7] & 0b1) == 0;
			boolean smoke_r = (wr[7] & 0b10) == 0;
			boolean smoke_g = (wr[7] & 0b100) == 0;
			boolean smoke_y = (wr[7] & 0b1000) == 0;
			boolean eloc = (wr[7] & 0b10000000) == 0;
			boolean claymore = (wr[8] & 0b1) == 0;
			boolean sgmine = (wr[8] & 0b10) == 0;
			boolean c4 = (wr[8] & 0b100) == 0;
			boolean sgsatchel = (wr[8] & 0b1000) == 0;
			boolean magazine = (wr[8] & 0b100000) == 0;
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
			boolean suppressor = (wr[9] & 0b100000) == 0;
			boolean gp30 = (wr[9] & 0b10000) == 0;
			boolean xm320 = (wr[9] & 0b1000) == 0;
			boolean masterkey = (wr[9] & 0b100) == 0;
			boolean scope = (wr[11] & 0b10000) == 0;
			boolean sight = (wr[11] & 0b100000) == 0;
			boolean laser = (wr[12] & 0b1) == 0;
			boolean lighthg = (wr[12] & 0b10) == 0;
			boolean lightlg = (wr[11] & 0b10000000) == 0;
			boolean grip = (wr[12] & 0b100) == 0;
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
			boolean envg = (wr[14] & 0b1000000) == 0;
			boolean drum = (wr[13] & 0b100) == 0;
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
			boolean scapExtraTime = (extraTimeFlags & 0b1) == 0;
			scap.addProperty("extraTime", scapExtraTime);

			JsonObject race = new JsonObject();
			ruleSettings.add("race", race);
			race.addProperty("time", raceTime);
			race.addProperty("rounds", raceRounds);
			boolean raceExtraTime = (extraTimeFlags & 0b100) == 0;
			race.addProperty("extraTime", raceExtraTime);

			String json = Util.jsonEncode(settings);
			user.setSessionHostSettings(json);

			if (saveSettings) {
				CharacterHostSettings hostSettings = settingsList.stream()
						.filter((e) -> e.getType() == lobby.getSubtype()).findFirst().orElse(null);
				if (hostSettings == null) {
					hostSettings = new CharacterHostSettings();
					hostSettings.setCharacter(character);
					hostSettings.setType(lobby.getSubtype());
					hostSettings.setSettings(null);

					settingsList.add(hostSettings);
				}

				hostSettings.setSettings(json);

				session = DB.getSession();
				session.beginTransaction();

				session.saveOrUpdate(hostSettings);

				session.getTransaction().commit();
				DB.closeSession(session);
			}

			Packets.write(ctx, 0x4311, 0);
		} catch (Exception e) {
			logger.error("Exception while updating host settings.", e);
			Packets.write(ctx, 0x4311, Error.GENERAL);
		}
	}

	public static void createGame(ChannelHandlerContext ctx, Lobby lobby) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while creating game: No User.");
				Packets.write(ctx, 0x4317, Error.INVALID_SESSION);
				return;
			}

			Character character = user.getCurrentCharacter();
			List<CharacterHostSettings> settingsList = character.getHostSettings();
			if (settingsList == null) {
				settingsList = new ArrayList<>();
				character.setHostSettings(settingsList);
			}

			String sessionHostSettings = user.getSessionHostSettings();

			JsonObject settings = Util.jsonDecode(sessionHostSettings);

			String name = settings.get("name").getAsString();
			String password = settings.get("password") != null && !settings.get("password").isJsonNull()
					? settings.get("password").getAsString() : null;
			int stance = settings.get("stance").getAsInt();
			String comment = settings.get("comment").getAsString();

			JsonArray games = settings.get("games").getAsJsonArray();
			JsonObject common = settings.get("common").getAsJsonObject();
			JsonObject ruleSettings = settings.get("ruleSettings").getAsJsonObject();

			int maxPlayers = common.get("maxPlayers").getAsInt();

			String jsonGames = Util.jsonEncode(games);
			String jsonCommon = Util.jsonEncode(common);
			String jsonRuleSettings = Util.jsonEncode(ruleSettings);

			Game game = new Game();
			game.setHostId(character.getId());
			game.setHost(character);
			game.setLobbyId(lobby.getId());
			game.setLobby(lobby);
			game.setName(name);
			game.setPassword(password);
			game.setComment(comment);
			game.setMaxPlayers(maxPlayers);
			game.setGames(jsonGames);
			game.setCommon(jsonCommon);
			game.setRules(jsonRuleSettings);
			game.setStance(stance);
			game.setCurrentGame(0);
			game.setLastUpdate((int) Instant.now().getEpochSecond());

			session = DB.getSession();
			session.beginTransaction();

			session.save(game);

			session.getTransaction().commit();
			DB.closeSession(session);

			game.initPlayers();
			Games.gameAddPlayer(game, character.getId(), false);
			NGames.add(game);

			bo = ctx.alloc().directBuffer(0x8);
			bo.writeInt(0).writeInt(game.getId());
			Packets.write(ctx, 0x4317, bo);

			EventCreateGame event = new EventCreateGame();
			event.setTime((int) Instant.now().getEpochSecond());
			event.setHostId(character.getId());
			event.setLobbyId(lobby.getId());
			event.setGameId(game.getId());
			event.setName(name);

			session = DB.getSession();
			session.beginTransaction();

			session.save(event);

			session.getTransaction().commit();
			DB.closeSession(session);

			logger.info("Created Game {} ({}).", game.getName(), game.getId());
		} catch (Exception e) {
			logger.error("Exception while creating game.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4317, Error.GENERAL);
		}
	}

	public static void setPlayerTeam(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while setting player team: No user.");
				Packets.write(ctx, 0x4345, Error.INVALID_SESSION);
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while setting player team: Not in a game.");
				Packets.writeError(ctx, 0x4345, 2);
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHostId())) {
				logger.error("Error while setting player team: Not the host.");
				Packets.writeError(ctx, 0x4345, 2);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetId = bi.readInt();
			int team = bi.readByte();

			Player targetPlayer = game.getPlayerByCharacterId(targetId);
			if (targetPlayer == null) {
				logger.error("Error while setting player team: Couldn't find player.");
				Packets.writeError(ctx, 0x4345, 1);
				return;
			}

			targetPlayer.setTeam(team);

			session = DB.getSession();
			session.beginTransaction();

			session.update(targetPlayer);

			session.getTransaction().commit();
			DB.closeSession(session);

			bo = ctx.alloc().directBuffer(0x8);

			bo.writeInt(0).writeInt(targetId);

			Packets.write(ctx, 0x4345, bo);
		} catch (Exception e) {
			logger.error("Exception while setting player team.", e);
			DB.rollbackAndClose(session);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4345, Error.GENERAL);
		}
	}

	public static void updatePings(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			Packets.write(ctx, 0x4399, 0);

			User user = NUsers.get(ctx.channel());
			Character character = user.getCurrentCharacter();

			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while setting player team: Not in a game.");
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHostId())) {
				logger.error("Error while setting player team: Not the host.");
				return;
			}

			game.setLastUpdate((int) Instant.now().getEpochSecond());

			ByteBuf bi = in.getPayload();
			int hostPing = bi.readInt();

			while (bi.readableBytes() >= 8) {
				int targetId = bi.readInt();
				int targetPing = bi.readInt();

				if (targetId == 0) {
					continue;
				}

				Player target = game.getPlayerByCharacterId(targetId);
				if (target != null) {
					target.setPing(targetPing);
				}
			}

			game.setPing(hostPing);

			session = DB.getSession();
			session.beginTransaction();

			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception while updating pings.", e);
			DB.rollbackAndClose(session);
		}

		Hosts.onPing(ctx);
	}

	public static void updateStats(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			Character character = user.getCurrentCharacter();

			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while updating stats: Not in a game.");
				Packets.writeError(ctx, 0x4391, 2);
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHostId())) {
				logger.error("Error while updating stats: Not the host.");
				Packets.writeError(ctx, 0x4391, 2);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetId = bi.readInt();
			bi.skipBytes(0x23);
			int experience = bi.readInt();
			bi.skipBytes(0x8c);
			boolean aborted = bi.readByte() == (byte) 0x01;

			Player targetPlayer = game.getPlayerByCharacterId(targetId);
			if (targetPlayer != null) {
				Character targetCharacter = targetPlayer.getCharacter();
				User targetUser = targetCharacter.getUser();

				int exp = 0;
				if (targetUser.getMainCharacterId() != null
						&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
					exp = targetUser.getMainExp();
				} else {
					exp = targetUser.getAltExp();
				}

				if (aborted) {
					experience = Math.max(0, exp - 60);
				}

				session = DB.getSession();
				session.beginTransaction();

				User aUser = session.get(User.class, targetUser.getId());
				if (targetUser.getMainCharacterId() != null
						&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
					aUser.setMainExp(experience);
				} else {
					aUser.setAltExp(experience);
				}

				session.getTransaction().commit();
				DB.closeSession(session);

				if (targetUser.getMainCharacterId() != null
						&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
					targetUser.setMainExp(experience);
				} else {
					targetUser.setAltExp(experience);
				}
			} else {
				List<Integer> playersLastRound = game.getPlayersLastRound();
				if (!playersLastRound.contains(targetId)) {
					logger.error("Error while updating stats: Player didn't play this round.");
					Packets.writeError(ctx, 0x4391, 3);
					return;
				}

				User targetUser = NUsers.getByCharacterId(targetId);
				if (targetUser != null) {
					Character targetCharacter = targetUser.getCurrentCharacter();

					int exp = 0;
					if (targetUser.getMainCharacterId() != null
							&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
						exp = targetUser.getMainExp();
					} else {
						exp = targetUser.getAltExp();
					}

					if (aborted) {
						experience = Math.max(0, exp - 60);
					}

					session = DB.getSession();
					session.beginTransaction();

					User aUser = session.get(User.class, targetUser.getId());
					if (targetUser.getMainCharacterId() != null
							&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
						aUser.setMainExp(experience);
					} else {
						aUser.setAltExp(experience);
					}

					session.getTransaction().commit();
					DB.closeSession(session);

					if (targetUser.getMainCharacterId() != null
							&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
						targetUser.setMainExp(experience);
					} else {
						targetUser.setAltExp(experience);
					}
				} else {
					session = DB.getSession();
					session.beginTransaction();

					Character targetCharacter = session.get(Character.class, targetId);
					if (targetCharacter == null) {
						logger.error("Error while updating stats: Character doesn't exist.");
					} else {
						targetUser = targetCharacter.getUser();

						int exp = 0;
						if (targetUser.getMainCharacterId() != null
								&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
							exp = targetUser.getMainExp();
						} else {
							exp = targetUser.getAltExp();
						}

						if (aborted) {
							experience = Math.max(0, exp - 60);
						}

						User aUser = session.get(User.class, targetUser.getId());
						if (targetUser.getMainCharacterId() != null
								&& targetCharacter.getId().equals(targetUser.getMainCharacterId())) {
							aUser.setMainExp(experience);
						} else {
							aUser.setAltExp(experience);
						}
					}

					session.getTransaction().commit();
					DB.closeSession(session);
				}
			}

			Packets.write(ctx, 0x4391, 0);
		} catch (Exception e) {
			logger.error("Exception while updating stats.", e);
			DB.rollbackAndClose(session);
			Packets.write(ctx, 0x4391, Error.GENERAL);
		}
	}

	public static void playerConnected(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while handling player connection: No user.");
				Packets.write(ctx, 0x4341, Error.INVALID_SESSION);
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while handling player connection: Not in a game.");
				Packets.writeError(ctx, 0x4341, 3);
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHost().getId())) {
				logger.error("Error while handling player connection: Not the host.");
				Packets.writeError(ctx, 0x4341, 4);
				return;
			}

			ByteBuf bi = in.getPayload();
			int targetId = bi.readInt();

			int result = Games.gameAddPlayer(game, targetId, true);
			if (result < 0) {
				Packets.writeError(ctx, 0x4341, 0xff + result);
				return;
			}

			bo = ctx.alloc().directBuffer(0x8);
			bo.writeInt(0).writeInt(targetId);
			Packets.write(ctx, 0x4341, bo);
		} catch (Exception e) {
			logger.error("Exception while handling player connection.", e);
			Util.releaseBuffer(bo);
			Packets.write(ctx, 0x4341, Error.GENERAL);
		}
	}

	public static void playerDisconnected(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		int targetId = 0;
		try {
			ByteBuf bi = in.getPayload();
			targetId = bi.readInt();

			bo = ctx.alloc().directBuffer(0x8);
			bo.writeInt(0).writeInt(targetId);
			Packets.write(ctx, 0x4343, bo);
		} catch (Exception e) {
			logger.error("Exception while handling player disconnection.", e);
			Util.releaseBuffer(bo);
		}
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while handling player disconnection: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while handling player disconnection: Not in a game.");
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHost().getId())) {
				logger.error("Error while handling player disconnection: Not the host.");
				return;
			}

			int result = Games.gameRemovePlayer(game, targetId, true);
			if (result < 0) {
				return;
			}
		} catch (Exception e) {
			logger.error("Exception while handling player disconnection.", e);
		}
	}

	public static void kickPlayer(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			ByteBuf bi = in.getPayload();
			int targetId = bi.readInt();

			bo = ctx.alloc().directBuffer(0x8);
			bo.writeInt(0).writeInt(targetId);

			Packets.write(ctx, 0x4347, bo);
		} catch (Exception e) {
			logger.error("Exception while handling player kick.", e);
			Util.releaseBuffer(bo);
		}
	}

	public static void setGame(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while setting game: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while setting game: Not in a game.");
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHost().getId())) {
				logger.error("Error while setting game: Not the host.");
				return;
			}

			ByteBuf bi = in.getPayload();
			int index = bi.readByte();

			game.setCurrentGame(index);

			session = DB.getSession();
			session.beginTransaction();

			session.update(game);

			session.getTransaction().commit();
			DB.closeSession(session);

			Packets.write(ctx, 0x4393, 0);
		} catch (Exception e) {
			logger.error("Exception while setting game.", e);
			// Fail silently
		}
	}

	public static void pass(ChannelHandlerContext ctx, Packet in) {
		Session session = null;
		try {
			Packets.write(ctx, 0x43a1, 0);

			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while setting game: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while passing game: Not in a game.");
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHost().getId())) {
				logger.error("Error while passing game: Not the host.");
				return;
			}

			ByteBuf bi = in.getPayload();
			int hostId = bi.readInt();
			int targetId = bi.readInt();

			Player targetPlayer = game.getPlayerByCharacterId(targetId);
			if (targetPlayer == null) {
				logger.error("Error while passing game: Couldn't find player.");
				return;
			}

			Character target = targetPlayer.getCharacter();

			game.setHostId(target.getId());
			game.setHost(target);

			Games.gameRemovePlayer(game, character.getId(), false);

			session = DB.getSession();
			session.beginTransaction();

			session.update(game);

			session.getTransaction().commit();
			DB.closeSession(session);
		} catch (Exception e) {
			logger.error("Exception while passing game.", e);
			DB.rollbackAndClose(session);
			// Fail silently
		}
	}

	public static void startRound(ChannelHandlerContext ctx) {
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while starting round: No user.");
				Packets.write(ctx, 0x43cb, Error.INVALID_SESSION);
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while starting round: Not in a game.");
				Packets.writeError(ctx, 0x43cb, 2);
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHostId())) {
				logger.error("Error while starting round: Not the host.");
				Packets.writeError(ctx, 0x43cb, 2);
				return;
			}

			List<Integer> playersLastRound = game.getPlayersLastRound();
			playersLastRound.clear();
			for (Player targetPlayer : game.getPlayers()) {
				playersLastRound.add(targetPlayer.getCharacterId());
			}

			Packets.write(ctx, 0x43cb, 0);
		} catch (Exception e) {
			logger.error("Exception while starting round.", e);
			Packets.write(ctx, 0x43cb, Error.GENERAL);
		}
	}

	public static void onPing(ChannelHandlerContext ctx) {
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while handling onPing: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while handling onPing: Not in a game.");
				return;
			}

			Game game = player.getGame();
			if (!character.getId().equals(game.getHostId())) {
				logger.error("Error while handling onPing: Not the host.");
				return;
			}

			int time = (int) Instant.now().getEpochSecond();
			if (time >= game.getLastNCheck() + 1 * 60) {
				PluginHandler.get().getPlugin().gameNCheck(game);
				game.setLastNCheck(time);
			}
		} catch (Exception e) {
			logger.error("Exception while handling onPing.", e);
		}
	}

}
