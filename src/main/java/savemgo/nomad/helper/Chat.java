package savemgo.nomad.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NChannels;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Chat {

	private static final Logger logger = LogManager.getLogger(Chat.class);

	private enum MessageRecipient {
		NORMAL, SELF, GLOBAL
	};

	private static ByteBuf constructMessage(ChannelHandlerContext ctx, int chara, int flag2, String message) {
		ByteBuf bb = ctx.alloc().directBuffer(message.length() + 6);
		bb.writeInt(chara).writeByte(flag2);
		Util.writeString(message, message.length() + 1, bb);
		return bb;
	}

	public static void send(ChannelHandlerContext ctx, Packet in) {
		try {
			User user = NUsers.get(ctx.channel());
			if (user == null) {
				logger.error("Error while sending message: No user.");
				return;
			}

			Character character = user.getCurrentCharacter();
			Player player = character.getPlayer().size() > 0 ? character.getPlayer().get(0) : null;
			if (player == null) {
				logger.error("Error while sending message: Not in a game.");
				return;
			}

			Game game = player.getGame();
			List<Player> players = game.getPlayers();

			ByteBuf bi = in.getPayload();
			int flag1 = bi.readByte();
			int flag2 = bi.readByte();
			String message = Util.readString(bi, 0x7f);

			if (message.startsWith("/all")) {
				message = message.replaceFirst("/all", "");
			} else if (message.startsWith("/team")) {
				message = message.replaceFirst("/team", "");
			}

			if (message.matches("(\\s+).*")) {
				message = message.replaceFirst("(\\s+)", "");
			}

			if (message.startsWith("Server |")) {
				message = message.replaceFirst("Server |", "");
			}

			MessageRecipient mr = MessageRecipient.NORMAL;

			if (message.startsWith("/nuser")) {
				message = "[NUser] " + user.getDisplayName() + " (" + user.getId() + ") - " + character.getName() + " ("
						+ character.getId() + ") " + "Game: " + game.getId() + " - Team: " + player.getTeam();
				mr = MessageRecipient.SELF;
			} else if (message.startsWith("/global ")) {
				if (user.getRole() >= 10) {
					message = "Server | " + message.replaceFirst("/global ", "");
					mr = MessageRecipient.GLOBAL;
				} else {
					message = "Server | You do not have permission to use this command.";
					mr = MessageRecipient.SELF;
				}
			}

			if (mr == MessageRecipient.SELF) {
				ByteBuf bo = null;
				try {
					bo = constructMessage(ctx, character.getId(), flag2, message);
					Packets.write(ctx, 0x4401, bo);
				} catch (Exception e) {
					logger.error("Exception during chat processing.", e);
					Util.releaseBuffer(bo);
					Packets.writeError(ctx, 0x4401, 1);
				}
			} else if (mr == MessageRecipient.GLOBAL) {
				final String fmessage = message;
				NChannels.process((ch) -> {
					try {
						User targetUser = NUsers.get(ch);
						Character targetCharacter = targetUser.getCurrentCharacter();
						return targetCharacter != null;
					} catch (Exception e) {
						logger.error("Exception during chat processing.", e);
					}
					return false;
				}, (ch) -> {
					ByteBuf bo = null;
					try {
						User targetUser = NUsers.get(ch);
						Character targetCharacter = targetUser.getCurrentCharacter();
						bo = constructMessage(ctx, targetCharacter.getId(), flag2, fmessage);
						Packets.write(ch, 0x4401, bo);
						Packets.flush(ch);
					} catch (Exception e) {
						logger.error("Exception during chat processing.", e);
						Util.releaseBuffer(bo);
						Packets.writeError(ctx, 0x4401, 1);
					}
				});
			} else {
				ArrayList<Player> recipients = new ArrayList<>(players);
				final String fmessage = message;
				NChannels.process((ch) -> {
					try {
						User targetUser = NUsers.get(ch);
						Character targetCharacter = targetUser.getCurrentCharacter();
						if (targetCharacter != null) {
							return recipients.stream().filter((e) -> e.getCharacterId() == targetCharacter.getId())
									.count() > 0;
						}
					} catch (Exception e) {
						logger.error("Exception during chat processing.", e);
					}
					return false;
				}, (ch) -> {
					ByteBuf bo = null;
					try {
						bo = constructMessage(ctx, character.getId(), flag2, fmessage);
						Packets.write(ch, 0x4401, bo);
						Packets.flush(ch);
					} catch (Exception e) {
						logger.error("Exception during chat processing.", e);
						Util.releaseBuffer(bo);
						Packets.writeError(ctx, 0x4401, 1);
					}
				});
			}
		} catch (Exception e) {
			logger.error("Exception while sending message.", e);
		}
	}

}
