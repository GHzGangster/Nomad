package savemgo.nomad.instance;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import savemgo.nomad.entity.Game;

public class NGames {

	private static final ConcurrentHashMap<Integer, Game> games = new ConcurrentHashMap<>();

	public static boolean initialize(Game game) {
		int id = game.getId();
		if (get(id) == null) {
			return games.put(id, game) == null;
		}
		return false;
	}

	public static boolean finalize(int gameId) {
		return games.remove(gameId) != null;
	}

	public static Game get(Predicate<Game> predicate) {
		return games.search(1, (key, value) -> predicate.test(value) ? value : null);
	}

	public static Game get(int gameId) {
		return games.search(1, (key, value) -> (key == gameId) ? value : null);
	}

	public static int getHost(int gameId) {
		Game game = get(gameId);
		if (game != null) {
			return game.getHost();
		}
		return 0;
	}

	public static void setHost(int gameId, int hostId) {
		Game game = get(gameId);
		if (game != null) {
			game.setHost(hostId);
		}
	}

	public static List<Integer> getPlayers(int gameId) {
		Game game = get(gameId);
		if (game != null) {
			return game.getPlayers();
		}
		return null;
	}

}
