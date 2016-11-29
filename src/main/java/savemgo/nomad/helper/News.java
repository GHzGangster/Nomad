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

public class News {

	private static final Logger logger = LogManager.getLogger(News.class.getSimpleName());

	public static void getNews(ChannelHandlerContext ctx) {
		// Sends 1 byte: 01
		
		Map<String, Object> response = Campbell.instance().getResponse("gate", "getNews");

		String result = TypeUtil.toString(response.get("result"));
		if (!result.equals("NOERR")) {
			logger.error("Error while getting news: " + result);
			ctx.write(new Packet(0x2009, 1));
			return;
		}

		ArrayList<Object> newsItems = TypeUtil.cast(response.get("news"));

		ctx.write(new Packet(0x2009, 0, true));

		for (Object o : newsItems) {
			LinkedTreeMap<String, Object> newsItem = TypeUtil.cast(o);

			int id = TypeUtil.toInt(newsItem.get("id"));
			int time = TypeUtil.toInt(newsItem.get("time"));
			int important = TypeUtil.toInt(newsItem.get("important"));
			String topic = TypeUtil.toString(newsItem.get("topic"));
			String message = TypeUtil.toString(newsItem.get("message"));

			message = message.substring(0, Math.min(message.length(), Packet.MAX_PAYLOAD_LENGTH - 138));

			ByteBuf payload = ctx.alloc().directBuffer(138 + message.length());

			payload.writeInt(id).writeByte(important).writeInt(time);
			Util.writeString(topic, 128, payload);
			Util.writeString(message, message.length() + 1, payload);

			ctx.write(new Packet(0x200a, payload));
		}

		ctx.write(new Packet(0x200b, 0, true));
	}

}
