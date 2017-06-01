package savemgo.nomad.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "mgo2_characters")
public class Character {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = true, unique = true)
	private Integer id;

	private Integer user;

	@Column(length = 16, nullable = false)
	private String name;

	private Integer rank = 0;

	private Integer exp = 0;

	private String comment;

	@Column(name = "host_score")
	private Integer hostScore = 0;

	@Column(name = "host_votes")
	private Integer hostVotes = 0;

	@Column(length = 2048, name = "gameplay_options")
	private String gameplayOptions;

	private Integer lobby = 0;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterAppearance> appearances;

	// OneToOne isn't working lazily, use OneToMany for now
	// @JoinColumn(name = "id")
	// @LazyToOne(LazyToOneOption.PROXY)
	// @OneToOne(fetch = FetchType.LAZY, mappedBy = "character", optional =
	// false)
	// private CharacterAppearance appearance;

	public Character() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getUser() {
		return user;
	}

	public void setUser(Integer user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public Integer getExp() {
		return exp;
	}

	public void setExp(Integer exp) {
		this.exp = exp;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getHostScore() {
		return hostScore;
	}

	public void setHostScore(Integer hostScore) {
		this.hostScore = hostScore;
	}

	public Integer getHostVotes() {
		return hostVotes;
	}

	public void setHostVotes(Integer hostVotes) {
		this.hostVotes = hostVotes;
	}

	public String getGameplayOptions() {
		return gameplayOptions;
	}

	public void setGameplayOptions(String gameplayOptions) {
		this.gameplayOptions = gameplayOptions;
	}

	public Integer getLobby() {
		return lobby;
	}

	public void setLobby(Integer lobby) {
		this.lobby = lobby;
	}

	public List<CharacterAppearance> getAppearances() {
		return appearances;
	}

	public void setAppearances(List<CharacterAppearance> appearances) {
		this.appearances = appearances;
	}

	// public CharacterAppearance getAppearance() {
	// return appearance;
	// }
	//
	// public void setAppearance(CharacterAppearance appearance) {
	// this.appearance = appearance;
	// }

}
