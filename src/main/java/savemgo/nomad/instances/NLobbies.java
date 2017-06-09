package savemgo.nomad.instances;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import savemgo.nomad.entity.Lobby;

public class NLobbies {

	private static final ConcurrentHashMap<Integer, Lobby> lobbies = new ConcurrentHashMap<>();

	public static void add(Lobby lobby) {
		lobbies.put(lobby.getId(), lobby);
	}

	public static void remove(Lobby lobby) {
		lobbies.remove(lobby);
	}

	public static ConcurrentHashMap<Integer, Lobby> get() {
		return lobbies;
	}

	public static Lobby get(Predicate<Lobby> predicate) {
		return lobbies.search(1, (key, value) -> predicate.test(value) ? value : null);
	}

	public static Lobby get(int id) {
		return lobbies.get(id);
	}

}
