package savemgo.nomad.entity;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = true, unique = true)
	private int id;

	@Column(length = 15, nullable = false)
	private String username;

	@Column(length = 32, name = "display_name", nullable = false)
	private String displayName;

	@Column(length = 255, nullable = false)
	private String email;

	@Column(length = 255, nullable = false)
	private String password;

	@Column(nullable = false)
	private int activated;

	@Column(nullable = false)
	private int role = 0;

	@Column(name = "banned_until", nullable = true)
	private Integer bannedUntil;

	@Column(length = 32, name = "mgo2_session", nullable = true)
	private String session;

	@Column(name = "mgo2_slots", nullable = false)
	private int slots = 3;

	@Column(name = "mgo2_chara", nullable = true)
	private Integer character;

	@Transient
	private int game = 0;

	@Transient
	private int gameJoining = 0;

	public User() {

	}

	public int getGame() {
		return game;
	}

	public void setGame(int game) {
		this.game = game;
	}

	public int getGameJoining() {
		return gameJoining;
	}

	public void setGameJoining(int gameJoining) {
		this.gameJoining = gameJoining;
	}

	public boolean isBanned() {
		if (bannedUntil > 0) {
			return bannedUntil > Instant.now().getEpochSecond();
		}
		return false;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
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

	public int getActivated() {
		return activated;
	}

	public void setActivated(int activated) {
		this.activated = activated;
	}

	public int getRole() {
		return role;
	}

	public void setRole(int role) {
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

	public int getSlots() {
		return slots;
	}

	public void setSlots(int slots) {
		this.slots = slots;
	}

	public Integer getCharacter() {
		return character;
	}

	public void setCharacter(Integer chara) {
		this.character = chara;
	}

}
