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
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "mgo2_clans")
public class Clan {

	@Column(nullable = false, unique = true)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Id
	private Integer id;

	@Column(length = 15, nullable = false)
	private String name;

	@Column(name = "leader", insertable = false, updatable = false)
	private Integer leaderId;

	@JoinColumn(name = "leader")
	@OneToOne(fetch = FetchType.LAZY, optional = true)
	private ClanMember leader;

	@Column(length = 128)
	private String comment;

	@Column(length = 512)
	private String notice;

	@Column(name = "notice_time")
	private Integer noticeTime;

	@Column(name = "notice_writer", nullable = false, insertable = false, updatable = false)
	private Integer noticeWriterId;

	@JoinColumn(name = "notice_writer")
	@OneToOne(fetch = FetchType.LAZY, optional = true)
	private ClanMember noticeWriter;

	@Column(name = "emblem_editor", insertable = false, updatable = false)
	private Integer emblemEditorId;

	@JoinColumn(name = "emblem_editor")
	@OneToOne(fetch = FetchType.LAZY, optional = true)
	private ClanMember emblemEditor;

	@Column
	@Lob
	private byte[] emblem;

	@Column(name = "emblem_wip")
	@Lob
	private byte[] emblemWip;

	@Column(nullable = false)
	private Integer open;

	@Version
	private Integer version;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "clan")
	private List<MessageClanApplication> applications;

	@OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "clan")
	private List<ClanMember> members;

	public Clan() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(Integer leaderId) {
		this.leaderId = leaderId;
	}

	public ClanMember getLeader() {
		return leader;
	}

	public void setLeader(ClanMember leader) {
		this.leader = leader;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getNotice() {
		return notice;
	}

	public void setNotice(String notice) {
		this.notice = notice;
	}

	public Integer getNoticeTime() {
		return noticeTime;
	}

	public void setNoticeTime(Integer noticeTime) {
		this.noticeTime = noticeTime;
	}

	public Integer getNoticeWriterId() {
		return noticeWriterId;
	}

	public void setNoticeWriterId(Integer noticeWriterId) {
		this.noticeWriterId = noticeWriterId;
	}

	public ClanMember getNoticeWriter() {
		return noticeWriter;
	}

	public void setNoticeWriter(ClanMember noticeWriter) {
		this.noticeWriter = noticeWriter;
	}

	public Integer getOpen() {
		return open;
	}

	public void setOpen(Integer open) {
		this.open = open;
	}

	public byte[] getEmblem() {
		return emblem;
	}

	public void setEmblem(byte[] emblem) {
		this.emblem = emblem;
	}

	public List<ClanMember> getMembers() {
		return members;
	}

	public void setMembers(List<ClanMember> members) {
		this.members = members;
	}

	public byte[] getEmblemWip() {
		return emblemWip;
	}

	public void setEmblemWip(byte[] emblemWip) {
		this.emblemWip = emblemWip;
	}

	public List<MessageClanApplication> getApplications() {
		return applications;
	}

	public void setApplications(List<MessageClanApplication> applications) {
		this.applications = applications;
	}

	public ClanMember getEmblemEditor() {
		return emblemEditor;
	}

	public void setEmblemEditor(ClanMember emblemEditor) {
		this.emblemEditor = emblemEditor;
	}

	public Integer getEmblemEditorId() {
		return emblemEditorId;
	}

	public void setEmblemEditorId(Integer emblemEditorId) {
		this.emblemEditorId = emblemEditorId;
	}

}
