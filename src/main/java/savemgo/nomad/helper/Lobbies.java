package savemgo.nomad.helper;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.internal.LinkedTreeMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import savemgo.nomad.TypeUtil;
import savemgo.nomad.Util;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.packet.Packet;

public class Lobbies {

	private static final Logger logger = LogManager.getLogger(Lobbies.class.getSimpleName());

	public static void getLobbyList(ChannelHandlerContext ctx) {
		ctx.write(new Packet(0x2002));

		Map<String, Object> response = Campbell.instance().getResponse("gate", "getLobbyList");

		String result = TypeUtil.toString(response.get("result"));
		if (!result.equals("NOERR")) {
			logger.error("Error while getting news: " + result);
			ctx.write(new Packet(0x2002, 1));
			return;
		}

		ArrayList<Object> lobbies = TypeUtil.cast(response.get("lobbies"));

		ByteBuf payload = ctx.alloc().directBuffer(46 * lobbies.size());

		int lobbyCtr = 0;
		for (Object o : lobbies) {
			LinkedTreeMap<String, Object> lobby = TypeUtil.cast(o);

			int id = TypeUtil.toInt(lobby.get("id"));
			int type = TypeUtil.toInt(lobby.get("type"));
			String name = TypeUtil.toString(lobby.get("name"));
			String ip = TypeUtil.toString(lobby.get("ip"));
			int port = TypeUtil.toInt(lobby.get("port"));
			int players = TypeUtil.toInt(lobby.get("players"));

			payload.writeInt(lobbyCtr++).writeInt(type);
			Util.writeString(name, 16, payload);
			Util.writeString(ip, 15, payload);
			payload.writeShort(port).writeShort(players).writeShort(id).writeByte(0);
		}

		ctx.write(new Packet(0x2003, payload));

		ctx.write(new Packet(0x2004));
	}

	public static void getGameLobbyList(ChannelHandlerContext ctx) {
		ctx.write(new Packet(0x4901, 0));

		Map<String, Object> response = Campbell.instance().getResponse("gate", "getGameLobbyList");

		String result = TypeUtil.toString(response.get("result"));
		if (!result.equals("NOERR")) {
			logger.error("Error while getting news: " + result);
			ctx.write(new Packet(0x2002, 1));
			return;
		}

		LinkedTreeMap<Integer, Object> lobbies = TypeUtil.cast(response.get("lobbies"));

		ByteBuf payload = ctx.alloc().directBuffer(35 * lobbies.size());

		for (Object o : lobbies.values()) {
			LinkedTreeMap<String, Object> lobby = TypeUtil.cast(o);

			int id = TypeUtil.toInt(lobby.get("id"));
			int subtype = TypeUtil.toInt(lobby.get("subtype"));
			int restriction = TypeUtil.toInt(lobby.get("restriction"));
			String name = TypeUtil.toString(lobby.get("name"));

			int attributes = (subtype << 24) | restriction;
			int openTime = 0, closeTime = 0, isOpen = 1;

			payload.writeInt(id).writeInt(attributes).writeShort(id);
			Util.writeString(name, 16, payload);
			payload.writeInt(openTime).writeInt(closeTime).writeByte(isOpen);
		}

		ctx.write(new Packet(0x4902, payload));

		ctx.write(new Packet(0x4903, 0));
	}

}
