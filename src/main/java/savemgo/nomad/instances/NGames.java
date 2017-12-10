package savemgo.nomad.instances;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import savemgo.nomad.entity.Game;

public class NGames {

	private static final ConcurrentHashMap<Integer, Game> games = new ConcurrentHashMap<>();

	public static boolean add(Game game) {
		int id = game.getId();
		if (get(id) == null) {
			return games.put(id, game) == null;
		}
		return false;
	}

	public static boolean remove(Game game) {
		return games.remove(game.getId()) != null;
	}
	
	public static boolean exists(Game game) {
		return games.contains(game);
	}

	public static Collection<Game> getGames() {
		return games.values();
	}
	
	public static Game get(Predicate<Game> predicate) {
		return games.search(1, (key, value) -> predicate.test(value) ? value : null);
	}

	public static Game get(int id) {
		return games.get(id);
	}

}
