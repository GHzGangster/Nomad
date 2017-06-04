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
@Table(name = "mgo2_characters_sets_skills")
public class CharacterSetSkills {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "chara", nullable = false, insertable = false, updatable = false)
	private Integer characterId;

	@JoinColumn(name = "chara")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Character character;

	@Column(name = "idx")
	private Integer index;

	@Column(length = 63)
	private String name = "";

	private Integer modes = 0;

	@Column(name = "skill_1", nullable = false)
	private Integer skill1;

	@Column(name = "skill_2", nullable = false)
	private Integer skill2;

	@Column(name = "skill_3", nullable = false)
	private Integer skill3;

	@Column(name = "skill_4", nullable = false)
	private Integer skill4;

	@Column(name = "level_1", nullable = false)
	private Integer level1;

	@Column(name = "level_2", nullable = false)
	private Integer level2;

	@Column(name = "level_3", nullable = false)
	private Integer level3;

	@Column(name = "level_4", nullable = false)
	private Integer level4;

	public CharacterSetSkills() {

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

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String text) {
		this.name = text;
	}

	public Integer getModes() {
		return modes;
	}

	public void setModes(Integer modes) {
		this.modes = modes;
	}

	public Integer getSkill1() {
		return skill1;
	}

	public void setSkill1(Integer skill1) {
		this.skill1 = skill1;
	}

	public Integer getSkill2() {
		return skill2;
	}

	public void setSkill2(Integer skill2) {
		this.skill2 = skill2;
	}

	public Integer getSkill3() {
		return skill3;
	}

	public void setSkill3(Integer skill3) {
		this.skill3 = skill3;
	}

	public Integer getSkill4() {
		return skill4;
	}

	public void setSkill4(Integer skill4) {
		this.skill4 = skill4;
	}

	public Integer getLevel1() {
		return level1;
	}

	public void setLevel1(Integer level1) {
		this.level1 = level1;
	}

	public Integer getLevel2() {
		return level2;
	}

	public void setLevel2(Integer level2) {
		this.level2 = level2;
	}

	public Integer getLevel3() {
		return level3;
	}

	public void setLevel3(Integer level3) {
		this.level3 = level3;
	}

	public Integer getLevel4() {
		return level4;
	}

	public void setLevel4(Integer level4) {
		this.level4 = level4;
	}

}
