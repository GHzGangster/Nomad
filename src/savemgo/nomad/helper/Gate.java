package savemgo.nomad.helper;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Map;

import savemgo.nomad.TypeUtil;
import savemgo.nomad.Util;
import savemgo.nomad.campbell.Campbell;
import savemgo.nomad.packet.Packet;

import com.google.gson.internal.LinkedTreeMap;

public class Gate {

	public static void getLobbyList(ChannelHandlerContext ctx) {
		ctx.write(new Packet(0x2002));

		Map<String, Object> response = Campbell.instance().getResponse("gate", "getLobbyList");
		
		String result = TypeUtil.toString(response.get("result"));
		if (!result.equals("NOERR")) {
			System.out.println("Error while getting news: " + result);
			ctx.write(new Packet(0x2002, 1));
			return;
		}
		
		ArrayList<Object> lobbies = TypeUtil.cast(response.get("lobbies"));

		ByteBuf payload = ctx.alloc().directBuffer(46 * lobbies.size());

		int lobbyCtr = 0;
		for (Object o : lobbies) {
			LinkedTreeMap<String, Object> lobby = TypeUtil.cast(o);

			int lobbyId = TypeUtil.toInt(lobby.get("id"));
			int lobbyType = TypeUtil.toInt(lobby.get("type"));
			String lobbyName = TypeUtil.toString(lobby.get("name"));
			String lobbyIP = TypeUtil.toString(lobby.get("ip"));
			int lobbyPort = TypeUtil.toInt(lobby.get("port"));
			int lobbyPlayers = TypeUtil.toInt(lobby.get("players"));

			payload.writeInt(lobbyCtr++).writeInt(lobbyType);
			Util.writeString(lobbyName, 16, payload);
			Util.writeString(lobbyIP, 15, payload);
			payload.writeShort(lobbyPort).writeShort(lobbyPlayers).writeShort(lobbyId).writeByte(0);
		}

		ctx.write(new Packet(0x2003, payload));

		ctx.write(new Packet(0x2004));
	}

	public static void getNews(ChannelHandlerContext ctx) {
		Map<String, Object> response = Campbell.instance().getResponse("gate", "getNews");
		
		String result = TypeUtil.toString(response.get("result"));
		if (!result.equals("NOERR")) {
			System.out.println("Error while getting news: " + result);
			ctx.write(new Packet(0x2009, 1));
			return;
		}
		
		ArrayList<Object> newsItems = TypeUtil.cast(response.get("news"));
		
		ctx.write(new Packet(0x2009));

		for (Object o : newsItems) {
			LinkedTreeMap<String, Object> newsItem = TypeUtil.cast(o);

			int id = TypeUtil.toInt(newsItem.get("id"));
			int time = TypeUtil.toInt(newsItem.get("time"));
			int important = TypeUtil.toInt(newsItem.get("important"));
			String topic = TypeUtil.toString(newsItem.get("topic"));
			String message = TypeUtil.toString(newsItem.get("message"));

			message = message.substring(0, Math.min(message.length(), 1023));

			ByteBuf payload = ctx.alloc().directBuffer(138 + message.length());

			payload.writeInt(id).writeByte(important).writeInt(time);
			Util.writeString(topic, 128, payload);
			Util.writeString(message, message.length() + 1, payload);

			ctx.write(new Packet(0x200a, payload));
		}

		ctx.write(new Packet(0x200b));
	}

}
