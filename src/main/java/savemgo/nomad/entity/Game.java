package savemgo.nomad.entity;

import java.util.ArrayList;

public class Game {

	private int id;

	private int host;

	private ArrayList<Integer> players;

	public Game() {

	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getHost() {
		return host;
	}

	public void setHost(int host) {
		this.host = host;
	}

	public ArrayList<Integer> getPlayers() {
		return players;
	}

}
