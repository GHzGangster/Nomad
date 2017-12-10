package savemgo.nomad.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.chat.ChatMessage;
import savemgo.nomad.chat.MessageRecipient;
import savemgo.nomad.entity.Character;
import savemgo.nomad.entity.Game;
import savemgo.nomad.entity.Player;
import savemgo.nomad.entity.User;
import savemgo.nomad.instances.NChannels;
import savemgo.nomad.instances.NGames;
import savemgo.nomad.instances.NUsers;
import savemgo.nomad.packet.Packet;
import savemgo.nomad.plugin.PluginHandler;
import savemgo.nomad.util.Error;
import savemgo.nomad.util.Packets;
import savemgo.nomad.util.Util;

public class Chat {

	private static final Logger logger = LogManager.getLogger(Chat.class);

	private static final String SERVER_MESSAGE_PREFIX = "Server | ";
	
	public static ByteBuf constructMessage(int chara, int flag2, String message) {
		ByteBuf bb = PooledByteBufAllocator.DEFAULT.directBuffer(message.length() + 6);
		bb.writeInt(chara).writeByte(flag2);
		Util.writeString(message, message.length() + 1, bb);
		return bb;
	}
	
	public static ByteBuf constructMessage(ChannelHandlerContext ctx, int chara, int flag2, String message) {
		ByteBuf bb = ctx.alloc().directBuffer(message.length() + 6);
		bb.writeInt(chara).writeByte(flag2);
		Util.writeString(message, message.length() + 1, bb);
		return bb;
	}

	public static void sendServerMessageToSelf(ChannelHandlerContext ctx, String message) {
		final String fmessage = SERVER_MESSAGE_PREFIX + message;
		ByteBuf bo = null;
		try {
			User targetUser = NUsers.get(ctx.channel());
			Character targetCharacter = targetUser.getCurrentCharacter();
			bo = Chat.constructMessage(ctx, targetCharacter.getId(), 0x30, fmessage);
			Packets.write(ctx, 0x4401, bo);
			Packets.flush(ctx);
		} catch (Exception e) {
			logger.error("Exception during chat processing.", e);
			Util.releaseBuffer(bo);
		}
	}
	
	public static void sendServerMessageToGame(String message, Game game) {
		final String fmessage = SERVER_MESSAGE_PREFIX + message;
		try {
			ArrayList<Player> recipients = new ArrayList<>(game.getPlayers());
			
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
					User targetUser = NUsers.get(ch);
					Character targetCharacter = targetUser.getCurrentCharacter();
					bo = constructMessage(targetCharacter.getId(), 0x30, fmessage);
					Packets.write(ch, 0x4401, bo);
					Packets.flush(ch);
				} catch (Exception e) {
					logger.error("Exception during chat processing.", e);
					Util.releaseBuffer(bo);
				}
			});
		} catch (Exception e) {
			logger.error("Exception during chat processing.", e);
		}
	}

	private static ChatMessage handleCommand(User user, String message) {
		if (message.startsWith("/global ")) {
			if (user.getRole() >= 10) {
				String out = message.replaceFirst("/global ", "");
				return new ChatMessage(MessageRecipient.GLOBAL, out);
			} else {
				return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
			}
		}
		
		if (message.startsWith("/room ")) {
			if (user.getRole() >= 10) {
				String out = message.replaceFirst("/room ", "");
				return new ChatMessage(MessageRecipient.ROOM, out);
			} else {
				return new ChatMessage(MessageRecipient.SELF, "You do not have permission to use this command.");
			}
		}

		if (message.startsWith("/kick ")) {
			String out;
			if (user.getRole() >= 10) {
				try {
					String idStr = message.replaceFirst("/kick ", "");
					int targetId = Integer.parseInt(idStr);
					NChannels.process((ch) -> {
						try {
							User targetUser = NUsers.get(ch);
							Character targetCharacter = targetUser.getCurrentCharacter();
							if (targetCharacter != null) {
								return targetCharacter.getId() == targetId;
							}
						} catch (Exception e) {
							logger.error("Exception during /kicking character.", e);
						}
						return false;
					}, (ch) -> {
						try {
							logger.info("/kicking: {}", Util.getUserInfo(ch));
							ch.close();
						} catch (Exception e) {
							logger.error("Exception during /kicking character", e);
						}
					});
					out = "Kicked character.";
				} catch (Exception e) {
					logger.error("Exception occurred while /kicking character", e);
					out = "Failed to kick character.";
				}
			} else {
				out = "You do not have permission to use this command.";
			}
			return new ChatMessage(MessageRecipient.SELF, out);
		}

		if (message.startsWith("/gamelog")) {
			String out;
			if (user.getRole() >= 10) {
				try {
					Collection<Game> games = NGames.getGames();
					for (Game aGame : games) {
						String lout = "GameLog | ";
						lout += aGame.getName() + " (" + aGame.getId() + ") | ";
						List<Player> aPlayers = aGame.getPlayers();
						for (Player aPlayer : aPlayers) {
							lout += aPlayer.getCharacter().getName() + " (" + aPlayer.getCharacterId() + "), ";
						}
						logger.info("{}", lout);
					}
					out = "Logged all game info.";
				} catch (Exception e) {
					out = "Failed to log info.";
				}
			} else {
				out = "You do not have permission to use this command.";
			}
			return new ChatMessage(MessageRecipient.SELF, out);
		}

		return null;
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

			ChatMessage chatMessage = null;
			chatMessage = PluginHandler.get().getPlugin().handleChatCommand(user, message);
			if (chatMessage == null) {
				chatMessage = handleCommand(user, message);
				if (chatMessage == null) {
					if (message.toLowerCase().startsWith(SERVER_MESSAGE_PREFIX.toLowerCase())) {
						chatMessage = new ChatMessage(MessageRecipient.SELF, "You can't send server messages, kiddo.");
					} else {
						chatMessage = new ChatMessage(MessageRecipient.NORMAL, message);
					}
				}
			}

			switch (chatMessage.getRecipient()) {
			case NORMAL: {
				ArrayList<Player> recipients = new ArrayList<>(players);
				final String fmessage = chatMessage.getMessage();
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
					}
				});
			}
				break;
			case SELF: {
				final String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();
				ByteBuf bo = null;
				try {
					bo = constructMessage(ctx, character.getId(), flag2, fmessage);
					Packets.write(ctx, 0x4401, bo);
				} catch (Exception e) {
					logger.error("Exception during chat processing.", e);
					Util.releaseBuffer(bo);
				}
			}
				break;
			case ROOM: {
				ArrayList<Player> recipients = new ArrayList<>(players);
				final String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();
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
					}
				});
			}
				break;
			case GLOBAL: {
				final String fmessage = SERVER_MESSAGE_PREFIX + chatMessage.getMessage();
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
					}
				});
			}
				break;
			}
		} catch (Exception e) {
			logger.error("Exception while sending message.", e);
		}
	}

}
