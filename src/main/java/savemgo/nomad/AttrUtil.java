package savemgo.nomad;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class AttrUtil {

	private static final AttributeKey<String> SESSION = AttributeKey.valueOf("session");
	
	public static String getSession(ChannelHandlerContext ctx) {
		Attribute<String> attr = ctx.channel().attr(SESSION);
		return attr.get();
	}
	
	public static void setSession(ChannelHandlerContext ctx, String session) {
		Attribute<String> attr = ctx.channel().attr(SESSION);
		attr.set(session);
	}
	
}
