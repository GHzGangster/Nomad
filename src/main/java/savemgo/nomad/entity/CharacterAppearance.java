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
@Table(name = "mgo2_characters_appearance")
public class CharacterAppearance {

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
	private Integer gender;

	@Column(nullable = false)
	private Integer face;

	@Column(nullable = false)
	private Integer voice;

	@Column(nullable = false)
	private Integer pitch;

	@Column(nullable = false)
	private Integer head;

	@Column(name = "head_color", nullable = false)
	private Integer headColor;

	@Column(nullable = false)
	private Integer upper;

	@Column(name = "upper_color", nullable = false)
	private Integer upperColor;

	@Column(nullable = false)
	private Integer lower;

	@Column(name = "lower_color", nullable = false)
	private Integer lowerColor;

	@Column(nullable = false)
	private Integer chest;

	@Column(name = "chest_color", nullable = false)
	private Integer chestColor;

	@Column(nullable = false)
	private Integer waist;

	@Column(name = "waist_color", nullable = false)
	private Integer waistColor;

	@Column(nullable = false)
	private Integer hands;

	@Column(name = "hands_color", nullable = false)
	private Integer handsColor;

	@Column(nullable = false)
	private Integer feet;

	@Column(name = "feet_color", nullable = false)
	private Integer feetColor;

	@Column(nullable = false)
	private Integer accessory1;

	@Column(name = "accessory1_color", nullable = false)
	private Integer accessory1Color;

	@Column(nullable = false)
	private Integer accessory2;

	@Column(name = "accessory2_color", nullable = false)
	private Integer accessory2Color;

	@Column(name = "face_paint", nullable = false)
	private Integer facePaint;

	@Version
	private Integer version;
	
	public CharacterAppearance() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Character getCharacter() {
		return character;
	}

	public void setCharacter(Character chara) {
		this.character = chara;
	}

	public Integer getGender() {
		return gender;
	}

	public void setGender(Integer gender) {
		this.gender = gender;
	}

	public Integer getFace() {
		return face;
	}

	public void setFace(Integer face) {
		this.face = face;
	}

	public Integer getVoice() {
		return voice;
	}

	public void setVoice(Integer voice) {
		this.voice = voice;
	}

	public Integer getPitch() {
		return pitch;
	}

	public void setPitch(Integer pitch) {
		this.pitch = pitch;
	}

	public Integer getHead() {
		return head;
	}

	public void setHead(Integer head) {
		this.head = head;
	}

	public Integer getHeadColor() {
		return headColor;
	}

	public void setHeadColor(Integer headColor) {
		this.headColor = headColor;
	}

	public Integer getUpper() {
		return upper;
	}

	public void setUpper(Integer upper) {
		this.upper = upper;
	}

	public Integer getUpperColor() {
		return upperColor;
	}

	public void setUpperColor(Integer upperColor) {
		this.upperColor = upperColor;
	}

	public Integer getLower() {
		return lower;
	}

	public void setLower(Integer lower) {
		this.lower = lower;
	}

	public Integer getLowerColor() {
		return lowerColor;
	}

	public void setLowerColor(Integer lowerColor) {
		this.lowerColor = lowerColor;
	}

	public Integer getChest() {
		return chest;
	}

	public void setChest(Integer chest) {
		this.chest = chest;
	}

	public Integer getChestColor() {
		return chestColor;
	}

	public void setChestColor(Integer chestColor) {
		this.chestColor = chestColor;
	}

	public Integer getWaist() {
		return waist;
	}

	public void setWaist(Integer waist) {
		this.waist = waist;
	}

	public Integer getWaistColor() {
		return waistColor;
	}

	public void setWaistColor(Integer waistColor) {
		this.waistColor = waistColor;
	}

	public Integer getHands() {
		return hands;
	}

	public void setHands(Integer hands) {
		this.hands = hands;
	}

	public Integer getHandsColor() {
		return handsColor;
	}

	public void setHandsColor(Integer handsColor) {
		this.handsColor = handsColor;
	}

	public Integer getFeet() {
		return feet;
	}

	public void setFeet(Integer feet) {
		this.feet = feet;
	}

	public Integer getFeetColor() {
		return feetColor;
	}

	public void setFeetColor(Integer feetColor) {
		this.feetColor = feetColor;
	}

	public Integer getAccessory1() {
		return accessory1;
	}

	public void setAccessory1(Integer accessory1) {
		this.accessory1 = accessory1;
	}

	public Integer getAccessory1Color() {
		return accessory1Color;
	}

	public void setAccessory1Color(Integer accessory1Color) {
		this.accessory1Color = accessory1Color;
	}

	public Integer getAccessory2() {
		return accessory2;
	}

	public void setAccessory2(Integer accessory2) {
		this.accessory2 = accessory2;
	}

	public Integer getAccessory2Color() {
		return accessory2Color;
	}

	public void setAccessory2Color(Integer accessory2Color) {
		this.accessory2Color = accessory2Color;
	}

	public Integer getFacePaint() {
		return facePaint;
	}

	public void setFacePaint(Integer facePaint) {
		this.facePaint = facePaint;
	}

	public Integer getCharacterId() {
		return characterId;
	}

	public void setCharacterId(Integer characterId) {
		this.characterId = characterId;
	}

}
