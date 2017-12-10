package savemgo.nomad.lobby;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.NomadLobby;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Lobby;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.helper.Characters;
import savemgo.nomad.helper.Chat;
import savemgo.nomad.helper.Clans;
import savemgo.nomad.helper.Games;
import savemgo.nomad.helper.Hosts;
import savemgo.nomad.helper.Hub;
import savemgo.nomad.helper.Messages;
import savemgo.nomad.helper.Users;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.plugin.PluginHandler;
import savemgo.nomad.util.Packets;

@Sharable
public class GameLobby extends NomadLobby {

	private static final Logger logger = LogManager.getLogger(GameLobby.class);

	public GameLobby(Lobby lobby) {
		super(lobby);
	}

	@Override
	public boolean handlePacket(ChannelHandlerContext ctx, Packet in) {
		int command = in.getCommand();

		int result = PluginHandler.get().getPlugin().handleGameLobbyCommand(ctx, in);
		if (result == 1) {
			return true;
		} else if (result == 0) {
			return false;
		}

		switch (command) {

		/** Accounts */
		case 0x3003:
			Users.checkSession(ctx, in, getLobby(), true);
			break;

		/** Characters */
		case 0x4100:
			// Get Profile Data
			Characters.getCharacterInfo(ctx);
			Characters.getGameplayOptionsUiSettings(ctx);
			Characters.getChatMacros(ctx);
			Characters.getPersonalInfo(ctx);
			Characters.getGear(ctx);
			Characters.getSkills(ctx);
			Characters.getSkillSets(ctx);
			Characters.getGearSets(ctx);
			break;

		case 0x4102:
			Characters.getPersonalStats(ctx, in);
			break;

		case 0x4110:
			Characters.updateGameplayOptions(ctx, in);
			break;

		case 0x4112:
			Characters.updateUiSettings(ctx, in);
			break;

		case 0x4114:
			Characters.updateChatMacros(ctx, in);
			break;

		case 0x4128:
			Characters.getPostGameInfo(ctx);
			break;

		case 0x4130:
			Characters.updatePersonalInfo(ctx, in);
			break;

		case 0x4141:
			Characters.updateSkillSets(ctx, in);
			break;

		case 0x4143:
			Characters.updateGearSets(ctx, in);
			break;

		case 0x4220:
			Characters.getCharacterCard(ctx, in);
			break;

		case 0x4500:
			Characters.addFriendsBlocked(ctx, in);
			break;

		case 0x4510:
			Characters.removeFriendsBlocked(ctx, in);
			break;

		case 0x4580:
			Characters.getFriendsBlockedList(ctx, in);
			break;

		case 0x4600:
			Characters.search(ctx, in);
			break;

		case 0x4680:
			Characters.getMatchHistory(ctx, in);
			break;

		case 0x4684:
			Characters.getOfficialGameHistory(ctx, in);
			break;

		case 0x4700:
			Characters.updateConnectionInfo(ctx, in);
			break;

		/** Mail */
		case 0x4820:
			Messages.getMessages(ctx, in);
			break;

		case 0x4840:
			Messages.getContents(ctx, in);
			break;

		/** Games */
		case 0x4300:
			Games.getList(ctx, getLobby(), 0x4301, in);
			// Games.getListFile(ctx, getLobby().getId(), 0x4301);
			break;

		case 0x4312:
			Games.getDetails(ctx, in, getLobby());
			// Games.getDetailsFile(ctx, in, getId());
			break;

		case 0x4320:
			Games.join(ctx, in);
			// Games.joinHostFile(ctx, in);
			break;

		case 0x4322:
			Games.joinFailed(ctx, in);
			break;

		/** Host */
		case 0x4304:
			Hosts.getSettings(ctx, getLobby());
			break;

		case 0x4310:
			Hosts.checkSettings(ctx, in, getLobby());
			break;

		case 0x4316:
			Hosts.createGame(ctx, getLobby());
			break;

		case 0x4340:
			Hosts.playerConnected(ctx, in);
			break;

		case 0x4342:
			Hosts.playerDisconnected(ctx, in);
			break;

		case 0x4344:
			Hosts.setPlayerTeam(ctx, in);
			break;

		case 0x4346:
			Hosts.kickPlayer(ctx, in);
			break;

		case 0x4380:
			Games.quitGame(ctx, true);
			break;

		case 0x4390:
			Hosts.updateStats(ctx, in);
			break;

		case 0x4394:
			// updategameenv
			logger.error("Update Game Environment not implemented.");
			break;

		case 0x4392:
			Hosts.setGame(ctx, in);
			break;

		case 0x4398:
			Hosts.updatePings(ctx, in);
			break;

		case 0x43a0:
			Hosts.pass(ctx, in);
			break;

		case 0x43a2:
			// Unknown, end of round, stats?
			Packets.write(ctx, 0x43a3, 0);
			break;

		case 0x43c0:
			// At start of hosted Training game, after Team join
			// Perhaps in-game information update?
			Packets.write(ctx, 0x43c1);
			break;

		case 0x43ca:
			// Start Round
			Hosts.startRound(ctx);
			break;

		/** Players */
		case 0x4400:
			Chat.send(ctx, in);
			break;

		case 0x4440:
			// Set Team
			Packets.write(ctx, 0x4441, 0);
			break;

		/** Hub */
		case 0x4150:
			Hub.onLobbyDisconnect(ctx, in);
			break;

		case 0x43d0:
			Hub.onTrainingConnect(ctx, in);
			break;

		case 0x4900:
			Hub.getGameLobbyInfo(ctx);
			break;

		case 0x4990:
			Hub.getGameEntryInfo(ctx);
			break;

		// case 0x4992:
		// When prompted to re-connect to a game, cancelled
		// In: ff ff ff ff, game id?

		// case 0x4914:
		// When prompted to re-connect to a game, cancelled

		/** Messages */
		case 0x4800:
			Messages.send(ctx, in);
			break;

		case 0x4860:
			Messages.addSent(ctx, in);
			break;

		/** Clans */

		case 0x4b00:
			Clans.create(ctx, in);
			break;

		case 0x4b04:
			Clans.disband(ctx);
			break;

		case 0x4b10:
			Clans.getList(ctx, in);
			break;

		case 0x4b20:
			Clans.getInformationMember(ctx, in);
			break;

		case 0x4b30:
			Clans.acceptJoin(ctx, in);
			break;

		case 0x4b32:
			Clans.declineJoin(ctx, in);
			break;

		case 0x4b36:
			Clans.banish(ctx, in);
			break;

		case 0x4b40:
			Clans.leave(ctx);
			break;

		case 0x4b42:
			Clans.apply(ctx, in);
			break;

		case 0x4b46:
			Clans.updateState(ctx, in);
			break;

		case 0x4b48:
			Clans.getEmblem(ctx, in, 0x4b49, false); // Lobby
			break;

		case 0x4b4a:
			Clans.getEmblem(ctx, in, 0x4b4b, false); // Normal
			break;

		case 0x4b4c:
			Clans.getEmblem(ctx, in, 0x4b4d, true); // Work-in-Progress
			break;

		case 0x4b50:
			Clans.setEmblem(ctx, in);
			break;

		case 0x4b52:
			Clans.getRoster(ctx, in);
			break;

		case 0x4b60:
			Clans.transferLeadership(ctx, in);
			break;

		case 0x4b62:
			Clans.setEmblemEditor(ctx, in);
			break;

		case 0x4b64:
			Clans.updateComment(ctx, in);
			break;

		case 0x4b66:
			Clans.updateNotice(ctx, in);
			break;

		case 0x4b70:
			Clans.getStats(ctx, in);
			break;

		case 0x4b80:
			Clans.getInformation(ctx, in);
			break;

		case 0x4b90:
			Clans.search(ctx, in);
			break;

		default:
			logger.error("Couldn't handle command " + Integer.toHexString(in.getCommand()));
			return false;
		}

		return true;
	}

	@Override
	public void onPing(ChannelHandlerContext ctx) {
		User user = NUsers.get(ctx.channel());
		if (user != null) {
			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player != null) {
				Game game = player.getGame();
				if (character.getId().equals(game.getHostId())) {
					Hosts.onPing(ctx);
				}
			}
		}
	}

	@Override
	public void onChannelInactive(ChannelHandlerContext ctx) {
		Users.onLobbyDisconnected(ctx, getLobby());
	}

}
