package savemgo.nomad.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "mgo2_players")
public class Player {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "game", nullable = false, insertable = false, updatable = false)
	private Integer gameId;

	@JoinColumn(name = "game")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Game game;

	@Column(name = "chara", nullable = false, insertable = false, updatable = false)
	private Integer characterId;

	@JoinColumn(name = "chara")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Character character;

	@Column(nullable = false)
	private Integer team = 0;

	@Column(nullable = false)
	private Integer ping = 0;

	@Version
	private Integer version;

	public Player() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getGameId() {
		return gameId;
	}

	public void setGameId(Integer gameId) {
		this.gameId = gameId;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public Integer getCharacterId() {
		return characterId;
	}

	public void setCharacterId(Integer characterId) {
		this.characterId = characterId;
	}

	public Character getCharacter() {
		return character;
	}

	public void setCharacter(Character character) {
		this.character = character;
	}

	public Integer getTeam() {
		return team;
	}

	public void setTeam(Integer team) {
		this.team = team;
	}

	public Integer getPing() {
		return ping;
	}

	public void setPing(Integer ping) {
		this.ping = ping;
	}

}
