package savemgo.nomad.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "mgo2_event_connectgame")
public class EventConnectGame {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "time", nullable = false)
	private Integer time;

	@Column(name = "game", nullable = false)
	private Integer gameId;

	@Column(name = "chara", nullable = false)
	private Integer charaId;	
	
	public EventConnectGame() {

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

	public Integer getCharaId() {
		return charaId;
	}

	public void setCharaId(Integer charaId) {
		this.charaId = charaId;
	}

}
