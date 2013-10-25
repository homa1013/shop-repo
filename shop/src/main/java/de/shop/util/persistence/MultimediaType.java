package de.shop.util.persistence;

public enum MultimediaType {
	IMAGE("I"),
	VIDEO("V"),
	AUDIO("A");

	private String dbString;
	
	private MultimediaType(String dbString) {
		this.dbString = dbString;
	}
	
	public String getDbString() {
		return dbString;
	}
	
	public static MultimediaType build(String dbString) {
		switch (dbString) {
			case "I":
				return IMAGE;
			case "V":
				return VIDEO;
			case "A":
				return AUDIO;
			default:
				throw new IllegalArgumentException(dbString + " ist kein gueltiger Wert fuer MultimediaType");
		}
	}
}
