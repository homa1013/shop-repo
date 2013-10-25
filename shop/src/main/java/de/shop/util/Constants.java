package de.shop.util;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
public final class Constants {
	public static final String REST_PATH = "/rest";

	// Header-Links
	public static final String SELF_LINK = "self";
	public static final String LIST_LINK = "list";
	public static final String ADD_LINK = "add";
	public static final String UPDATE_LINK = "update";
	public static final String REMOVE_LINK = "remove";
	public static final String FIRST_LINK = "first";
	public static final String LAST_LINK = "last";
	
	// JPA
	public static final Long KEINE_ID = null;
	public static final long MIN_ID = 1L;
	public static final int ERSTE_VERSION = 0;
	public static final int MAX_AUTOCOMPLETE = 10;
	
	// JAAS
	public static final String SECURITY_DOMAIN = "shop";
	
	public static final String HASH_ALGORITHM = "SHA-256";
	public static final String HASH_ENCODING = "base64";
	public static final String HASH_CHARSET = "UTF-8";
	
	private Constants() {
	}
}
