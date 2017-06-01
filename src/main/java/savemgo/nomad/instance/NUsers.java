package savemgo.nomad.instance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import io.netty.channel.Channel;
import savemgo.nomad.entity.User;

public class NUsers {

	private static final ConcurrentHashMap<Channel, User> users = new ConcurrentHashMap<>();

	public static boolean initialize(Channel channel, User user) {
		if (get(channel) == null) {
			return users.put(channel, user) == null;
		}
		return false;
	}

	public static boolean finalize(Channel channel) {
		return users.remove(channel) != null;
	}

	public static User get(Channel channel) {
		return users.get(channel);
	}

	public static User get(Predicate<User> predicate) {
		return users.search(1, (key, value) -> predicate.test(value) ? value : null);
	}

	public static User getByCharacter(int charaId) {
		return get((e) -> (Integer) e.getCharacter() == charaId);
	}

}
