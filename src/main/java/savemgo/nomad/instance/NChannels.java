package savemgo.nomad.instance;

import java.util.function.Consumer;
import java.util.function.Predicate;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class NChannels {

	private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

	public static void add(Channel ch) {
		channels.add(ch);
	}

	public static void remove(Channel ch) {
		channels.remove(ch);
	}

	public static void process(Predicate<Channel> predicate, Consumer<Channel> consumer) {
		for (Channel channel : channels) {
			if (predicate.test(channel)) {
				consumer.accept(channel);
			}
		}
	}

}
