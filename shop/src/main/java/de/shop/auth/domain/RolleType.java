package de.shop.auth.domain;

public enum RolleType {
	ADMIN("admin"),
	MITARBEITER("mitarbeiter"),
	ABTEILUNGSLEITER("abteilungsleiter"),
	KUNDE("kunde");
	
private String internal;
	
	private RolleType(String internal) {
		this.internal = internal;
	}
	
	public String getInternal() {
		return internal;
	}
	
	public static RolleType build(String internal) {
		if (internal == null) {
			return null;
		}
		
		return RolleType.valueOf(internal.toUpperCase());
	}
}
