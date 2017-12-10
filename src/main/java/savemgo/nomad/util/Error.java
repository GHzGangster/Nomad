package savemgo.nomad.util;

public enum Error {

	/** General */
	NOT_IMPLEMENTED(0xff),
	GENERAL(0x1),
	INVALID_SESSION(0x2),
	
	/** User */
	CHAR_NAMEINVALID(0x10),
	CHAR_NAMEPREFIX(0x11),
	CHAR_NAMERESERVED(0x12),
	CHAR_NAMETAKEN(-260, true), // fffffefc
	CHAR_CANTDELETEYET(0x14),
	
	/** Character */
	CHARACTER_DOESNOTEXIST(0x20),
	
	/** Game */
	GAME_PLACEHOLDER(0x30),
	
	/** Clan */
	CLAN_DOESNOTEXIST(0x40),
	CLAN_NOTAMEMBER(0x41),
	CLAN_INACLAN(0x42),
	CLAN_NOAPPLICATION(0x43),
	CLAN_HASAPPLICATION(0x44),
	CLAN_NOTALEADER(0x45);
	
	
	private int code;
	private boolean official = false;
	
	Error(int code) {
		this.code = code;
	}
	
	Error(int code, boolean official) {
		this.code = code;
		this.official = official;
	}
	
	public int getCode() {
		return code;
	}

	public boolean isOfficial() {
		return official;
	}	
	
}
