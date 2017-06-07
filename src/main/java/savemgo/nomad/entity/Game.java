package savemgo.nomad.entity;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "mgo2_games")
public class Game {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "host", nullable = false, insertable = false, updatable = false)
	private Integer hostId;

	@JoinColumn(name = "host")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Character host;

	@Column(name = "lobby", nullable = false, insertable = false, updatable = false)
	private Integer lobbyId;

	@JoinColumn(name = "lobby")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Lobby lobby;

	@Column(length = 16, nullable = false)
	private String name;

	@Column(length = 15)
	private String password;

	@Column(length = 128, nullable = false)
	private String comment = "Good luck.";

	@Column(name = "max_players", nullable = false)
	private Integer maxPlayers;

	@Column(name = "current_game", nullable = false)
	private Integer currentGame = 0;

	@Column(length = 180, nullable = false)
	private String games;

	@Column(nullable = false)
	private Integer stance = 0;

	@Column(nullable = false)
	private Integer ping = 0;

	@Column(length = 1300, nullable = false)
	private String common;

	@Column(length = 600, nullable = false)
	private String rules;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "game", orphanRemoval = true)
	private List<Player> players;

	public Game() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getMaxPlayers() {
		return maxPlayers;
	}

	public void setMaxPlayers(Integer maxPlayers) {
		this.maxPlayers = maxPlayers;
	}

	public Integer getCurrentGame() {
		return currentGame;
	}

	public void setCurrentGame(Integer currentGame) {
		this.currentGame = currentGame;
	}

	public String getGames() {
		return games;
	}

	public void setGames(String games) {
		this.games = games;
	}

	public Integer getStance() {
		return stance;
	}

	public void setStance(Integer stance) {
		this.stance = stance;
	}

	public Integer getPing() {
		return ping;
	}

	public void setPing(Integer ping) {
		this.ping = ping;
	}

	public String getCommon() {
		return common;
	}

	public void setCommon(String common) {
		this.common = common;
	}

	public String getRules() {
		return rules;
	}

	public void setRules(String rules) {
		this.rules = rules;
	}

	public Character getHost() {
		return host;
	}

	public void setHost(Character host) {
		this.host = host;
	}

	public Lobby getLobby() {
		return lobby;
	}

	public void setLobby(Lobby lobby) {
		this.lobby = lobby;
	}

	public List<Player> getPlayers() {
		return players;
	}

	public void setPlayers(List<Player> players) {
		this.players = players;
	}

}
