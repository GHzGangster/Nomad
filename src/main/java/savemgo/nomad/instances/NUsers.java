package savemgo.nomad.instances;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import savemgo.nomad.entity.User;

public class NUsers {

	private static final ConcurrentHashMap<Channel, User> users = new ConcurrentHashMap<>();

	public static boolean add(Channel channel, User user) {
		if (get(channel) == null) {
			return users.put(channel, user) == null;
		}
		return false;
	}

	public static boolean remove(Channel channel) {
		return users.remove(channel) != null;
	}

	public static User get(Channel channel) {
		return users.get(channel);
	}

	public static List<User> get(Predicate<User> predicate) {
		return users.values().stream().filter(predicate).collect(Collectors.toList());
	}

	public static User getOne(Predicate<User> predicate) {
		return users.search(1, (key, value) -> predicate.test(value) ? value : null);
	}

	public static User getByCharacterId(int charaId) {
		return getOne((e) -> e != null && e.getCurrentCharacterId() != null
				&& (Integer) e.getCurrentCharacterId() == charaId);
	}

}
