package de.shop.artikelverwaltung.service;

import de.shop.util.AbstractShopException;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
public abstract class AbstractArtikelServiceException extends AbstractShopException {
	private static final long serialVersionUID = 5999208465631860486L;

	public AbstractArtikelServiceException(String msg) {
		super(msg);
	}

	public AbstractArtikelServiceException(String msg, Throwable t) {
		super(msg, t);
	}
}
