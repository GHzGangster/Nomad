package savemgo.nomad.entity;

import java.time.Instant;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import io.netty.channel.Channel;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = true, unique = true)
	private Integer id;

	@Column(length = 15, nullable = false)
	private String username;

	@Column(length = 32, name = "display_name", nullable = false)
	private String displayName;

	@Column(length = 255, nullable = false)
	private String email;

	@Column(length = 255, nullable = false)
	private String password;

	@Column(nullable = false)
	private Integer activated;

	@Column(nullable = false)
	private Integer role = 0;

	@Column(name = "banned_until", nullable = true)
	private Integer bannedUntil;

	@Column(name = "ban_reason", nullable = true)
	private String banReason;

	@Column(length = 32, name = "mgo2_session", nullable = true)
	private String session;

	@Column(name = "mgo2_session_iscfw", nullable = false)
	private Boolean sessionIsCfw;

	@Column(name = "mgo2_slots", nullable = false)
	private Integer slots = 3;

	@Column(name = "mgo2_chara", nullable = true, updatable = false, insertable = false)
	private Integer currentCharacterId;

	@JoinColumn(name = "mgo2_chara", nullable = true)
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private Character currentCharacter;

	@Column(name = "mgo2_main", nullable = true, updatable = false, insertable = false)
	private Integer mainCharacterId;

	@JoinColumn(name = "mgo2_main", nullable = true)
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	private Character mainCharacter;

	@Column(name = "mgo2_main_exp", nullable = false)
	private Integer mainExp = 0;

	@Column(name = "mgo2_alt_exp", nullable = false)
	private Integer altExp = 0;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "user", orphanRemoval = true)
	private List<Character> characters;

	@Version
	private Integer version;

	@Transient
	private Channel channel;

	@Transient
	private String sessionHostSettings;

	public User() {

	}

	public boolean isBanned() {
		if (bannedUntil != null && bannedUntil > 0) {
			return bannedUntil > Instant.now().getEpochSecond();
		}
		return false;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Integer getActivated() {
		return activated;
	}

	public void setActivated(Integer activated) {
		this.activated = activated;
	}

	public Integer getRole() {
		return role;
	}

	public void setRole(Integer role) {
		this.role = role;
	}

	public Integer getBannedUntil() {
		return bannedUntil;
	}

	public void setBannedUntil(Integer bannedUntil) {
		this.bannedUntil = bannedUntil;
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public Integer getSlots() {
		return slots;
	}

	public void setSlots(Integer slots) {
		this.slots = slots;
	}

	public Integer getCurrentCharacterId() {
		return currentCharacterId;
	}

	public void setCurrentCharacterId(Integer currentCharacterId) {
		this.currentCharacterId = currentCharacterId;
	}

	public Character getCurrentCharacter() {
		return currentCharacter;
	}

	public void setCurrentCharacter(Character currentCharacter) {
		this.currentCharacter = currentCharacter;
	}

	public List<Character> getCharacters() {
		return characters;
	}

	public void setCharacters(List<Character> characters) {
		this.characters = characters;
	}

	public String getBanReason() {
		return banReason;
	}

	public void setBanReason(String banReason) {
		this.banReason = banReason;
	}

	public Boolean getSessionIsCfw() {
		return sessionIsCfw;
	}

	public void setSessionIsCfw(Boolean sessionIsCfw) {
		this.sessionIsCfw = sessionIsCfw;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public String getSessionHostSettings() {
		return sessionHostSettings;
	}

	public void setSessionHostSettings(String sessionHostSettings) {
		this.sessionHostSettings = sessionHostSettings;
	}

	public Integer getMainCharacterId() {
		return mainCharacterId;
	}

	public void setMainCharacterId(Integer mainCharacterId) {
		this.mainCharacterId = mainCharacterId;
	}

	public Character getMainCharacter() {
		return mainCharacter;
	}

	public void setMainCharacter(Character mainCharacter) {
		this.mainCharacter = mainCharacter;
	}

	public Integer getMainExp() {
		return mainExp;
	}

	public void setMainExp(Integer mainExp) {
		this.mainExp = mainExp;
	}

	public Integer getAltExp() {
		return altExp;
	}

	public void setAltExp(Integer altExp) {
		this.altExp = altExp;
	}

}
