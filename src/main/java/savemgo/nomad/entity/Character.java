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
import javax.persistence.Transient;
import javax.persistence.Version;

@Entity
@Table(name = "mgo2_characters")
public class Character {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = true, unique = true)
	private Integer id;

	@Column(name = "user", nullable = true, insertable = false, updatable = false)
	private Integer userId;

	@JoinColumn(name = "user")
	@OneToOne(fetch = FetchType.LAZY)
	private User user;

	@Column(length = 16, nullable = false)
	private String name;

	@Column(name = "old_name", length = 16, nullable = true)
	private String oldName;

	private Integer rank = 0;

	@Column(length = 128)
	private String comment;

	@Column(name = "host_score")
	private Integer hostScore = 0;

	@Column(name = "host_votes")
	private Integer hostVotes = 0;

	@Column(length = 2048, name = "gameplay_options")
	private String gameplayOptions;

	@Column(name = "creation_time")
	private Integer creationTime;

	private Integer active;

	@Column(name = "lobby", nullable = true, insertable = false, updatable = false)
	private Integer lobbyId = 0;

	@JoinColumn(name = "lobby", nullable = true)
	@OneToOne(fetch = FetchType.LAZY)
	private Lobby lobby;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterAppearance> appearance;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterBlocked> blocked;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterChatMacro> chatMacros;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<MessageClanApplication> clanApplication;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<ClanMember> clanMember;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterEquippedSkills> skills;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterFriend> friends;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterHostSettings> hostSettings;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterSetGear> setsGear;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<CharacterSetSkills> setsSkills;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<ConnectionInfo> connectionInfo;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "character")
	private List<Player> player;

	// OneToOne isn't working lazily, use OneToMany for now
	// @JoinColumn(name = "id")
	// @LazyToOne(LazyToOneOption.PROXY)
	// @OneToOne(fetch = FetchType.LAZY, mappedBy = "character", optional =
	// false)
	// private CharacterAppearance appearance;

	// @Version
	// private Integer version;

	@Transient
	private Integer gameJoining = null;

	public Character() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setUser(Integer user) {
		this.userId = user;
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

	public Integer getLobbyId() {
		return lobbyId;
	}

	public void setLobbyId(Integer lobby) {
		this.lobbyId = lobby;
	}

	public List<CharacterAppearance> getAppearance() {
		return appearance;
	}

	public void setAppearance(List<CharacterAppearance> appearance) {
		this.appearance = appearance;
	}

	public Integer getUserId() {
		return userId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<CharacterFriend> getFriends() {
		return friends;
	}

	public void setFriends(List<CharacterFriend> friends) {
		this.friends = friends;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public List<CharacterBlocked> getBlocked() {
		return blocked;
	}

	public void setBlocked(List<CharacterBlocked> blocked) {
		this.blocked = blocked;
	}

	public List<CharacterChatMacro> getChatMacros() {
		return chatMacros;
	}

	public void setChatMacros(List<CharacterChatMacro> chatMacros) {
		this.chatMacros = chatMacros;
	}

	public List<CharacterEquippedSkills> getSkills() {
		return skills;
	}

	public void setSkills(List<CharacterEquippedSkills> skills) {
		this.skills = skills;
	}

	public List<CharacterSetSkills> getSetsSkills() {
		return setsSkills;
	}

	public void setSetsSkills(List<CharacterSetSkills> setsSkills) {
		this.setsSkills = setsSkills;
	}

	public List<ConnectionInfo> getConnectionInfo() {
		return connectionInfo;
	}

	public void setConnectionInfo(List<ConnectionInfo> connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

	public List<CharacterSetGear> getSetsGear() {
		return setsGear;
	}

	public void setSetsGear(List<CharacterSetGear> setsGear) {
		this.setsGear = setsGear;
	}

	public List<CharacterHostSettings> getHostSettings() {
		return hostSettings;
	}

	public void setHostSettings(List<CharacterHostSettings> hostSettings) {
		this.hostSettings = hostSettings;
	}

	public Lobby getLobby() {
		return lobby;
	}

	public void setLobby(Lobby lobby) {
		this.lobby = lobby;
	}

	public Integer getGameJoining() {
		return gameJoining;
	}

	public void setGameJoining(Integer gameJoining) {
		this.gameJoining = gameJoining;
	}

	public List<Player> getPlayer() {
		return player;
	}

	public void setPlayer(List<Player> player) {
		this.player = player;
	}

	public Integer getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Integer creationTime) {
		this.creationTime = creationTime;
	}

	public String getOldName() {
		return oldName;
	}

	public void setOldName(String oldName) {
		this.oldName = oldName;
	}

	public Integer getActive() {
		return active;
	}

	public void setActive(Integer active) {
		this.active = active;
	}

	public List<ClanMember> getClanMember() {
		return clanMember;
	}

	public void setClanMember(List<ClanMember> clanMember) {
		this.clanMember = clanMember;
	}

	public List<MessageClanApplication> getClanApplication() {
		return clanApplication;
	}

	public void setClanApplication(List<MessageClanApplication> clanApplication) {
		this.clanApplication = clanApplication;
	}

}
