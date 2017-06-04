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

@Entity
@Table(name = "mgo2_characters_hostsettings")
public class CharacterHostSettings {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "chara", nullable = false, insertable = false, updatable = false)
	private Integer characterId;

	@JoinColumn(name = "chara")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Character character;

	@Column(nullable = false)
	private Integer type;

	@Column(nullable = false)
	private String settings;

	public CharacterHostSettings() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}
	
}
