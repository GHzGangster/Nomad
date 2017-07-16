package savemgo.nomad.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mgo2_event_creategame")
public class EventCreateGame {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "time", nullable = false)
	private Integer time;

	@Column(name = "game", nullable = false)
	private Integer gameId;

	@Column(name = "host", nullable = false)
	private Integer hostId;

	@Column(name = "lobby", nullable = false)
	private Integer lobbyId;
	
	@Column(nullable = false)
	private String name;

	public EventCreateGame() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getTime() {
		return time;
	}

	public void setTime(Integer time) {
		this.time = time;
	}

	public Integer getGameId() {
		return gameId;
	}

	public void setGameId(Integer gameId) {
		this.gameId = gameId;
	}

	public Integer getHostId() {
		return hostId;
	}

	public void setHostId(Integer hostId) {
		this.hostId = hostId;
	}

	public Integer getLobbyId() {
		return lobbyId;
	}

	public void setLobbyId(Integer lobbyId) {
		this.lobbyId = lobbyId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
