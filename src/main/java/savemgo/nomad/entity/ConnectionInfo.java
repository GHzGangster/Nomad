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
@Table(name = "mgo2_connections")
public class ConnectionInfo {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(name = "chara", nullable = false, insertable = false, updatable = false)
	private Integer characterId;

	@JoinColumn(name = "chara")
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	private Character character;

	@Column(name = "public_ip", nullable = false)
	private String publicIp;
	
	@Column(length = 15, name = "public_port", nullable = false)
	private Integer publicPort;

	@Column(name = "private_ip", nullable = false)
	private String privateIp;
	
	@Column(length = 15, name = "private_port", nullable = false)
	private Integer privatePort;	
	
	public ConnectionInfo() {

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

	public String getPublicIp() {
		return publicIp;
	}

	public void setPublicIp(String publicIp) {
		this.publicIp = publicIp;
	}

	public Integer getPublicPort() {
		return publicPort;
	}

	public void setPublicPort(Integer publicPort) {
		this.publicPort = publicPort;
	}

	public String getPrivateIp() {
		return privateIp;
	}

	public void setPrivateIp(String privateIp) {
		this.privateIp = privateIp;
	}

	public Integer getPrivatePort() {
		return privatePort;
	}

	public void setPrivatePort(Integer privatePort) {
		this.privatePort = privatePort;
	}
	
}
