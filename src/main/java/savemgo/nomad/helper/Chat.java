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
import savemgo.nomad.instance.NChannels;
import savemgo.nomad.instance.NGames;
import savemgo.nomad.instance.NUsers;
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

	public static void onMessage(ChannelHandlerContext ctx, Packet in) {
		ByteBuf bo = null;
		try {
			int chara = NUsers.getCharacter(ctx);

			ByteBuf bi = in.getPayload();
			int flag1 = bi.readByte();
			int flag2 = bi.readByte();
			String message = Util.readString(bi, 0x7f);

			if (message.startsWith("/all")) {
				message = message.replaceFirst("/all", "");
			} else if (message.startsWith("/team")) {
				message = message.replaceFirst("/team", "");
			}

			if (message.startsWith(" ")) {
				message = message.replaceFirst(" ", "");
			}

			MessageRecipient mr = MessageRecipient.NORMAL;

			if (message.startsWith("/nuser")) {
				Supplier<String> s = () -> {
					ConcurrentMap<String, Object> player = NUsers.get(ctx.channel());
					if (player == null) {
						return "[NUser] Failed to get instance.";
					}

					int user = (Integer) player.get("user");
					int userRole = (Integer) player.get("userRole");
					String name = (String) player.get("charaName");
					int game = (Integer) player.get("game");
					
					return "[NUser] User: " + user + " Role: " + userRole + " Chara: " + chara + " Name: " + name
							+ " Game: " + game;
				};
				s.get();
				message = s.get();
				mr = MessageRecipient.SELF;
			} else if (message.startsWith("/ngame")) {
				Supplier<String> s = () -> {
					int gameId = NUsers.getGame(ctx);
					
					ConcurrentMap<String, Object> game = NGames.get(gameId);
					if (game == null) {
						return "[NGame] Failed to get instance.";
					}

					int host = (Integer) game.get("host");
					
					List<Integer> players = Util.cast(game.get("players"));
					
					String playersStr = "";
					for (Integer player : players) {
						playersStr += player + " ";
					}
					
					return "[NGame] ID: " + gameId + " Host: " + host + " Players: " + playersStr;
				};
				s.get();
				message = s.get();
				mr = MessageRecipient.SELF;
			}

			switch (mr) {
			case SELF:
				bo = constructMessage(ctx, chara, flag2, message);
				Packets.write(ctx, 0x4401, bo);
				break;
			case GLOBAL:

			default:
				bo = constructMessage(ctx, chara, flag2, message);

				ArrayList<Integer> recipients = new ArrayList<>();
				recipients.add(chara);

				final ByteBuf _bo = bo;

				NChannels.process((ch) -> {
					ConcurrentHashMap<String, Object> info = NUsers.get(ch);
					Integer character = (Integer) info.get("chara");
					if (character != null && character != 0) {
						return recipients.stream().filter(e -> e.intValue() == character.intValue()).count() > 0;
					}
					return false;
				}, (ch) -> {
					Packets.write(ch, 0x4401, _bo);
					Packets.flush(ch);
				});
				break;
			}
		} catch (Exception e) {
			logger.error("Exception while handling chat message.", e);
			Packets.writeError(ctx, 0x4401, 1);
			Util.releaseBuffer(bo);
		}
	}

}
