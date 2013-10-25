package de.shop.bestellverwaltung.service;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Lieferung;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.util.interceptor.Log;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Decorator
@Log
public abstract class BestellungServiceMitGeschenkverpackung implements BestellungService {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());

	@Inject
	@Delegate
	private BestellungService bs;

	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung findBestellungById(Long id, FetchType fetch) {
		return bs.findBestellungById(id, fetch);
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public List<Bestellung> findBestellungenByKunde(AbstractKunde kunde, FetchType fetch) {
		return bs.findBestellungenByKunde(kunde, fetch);
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public AbstractKunde findKundeById(Long id) {
		return bs.findKundeById(id);
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung createBestellung(Bestellung bestellung, String username) {
		LOGGER.warn("Geschenkverpackung noch nicht implementiert");
		return bs.createBestellung(bestellung, username);
	}
	
	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung createBestellung(Bestellung bestellung, AbstractKunde kunde) {
		LOGGER.warn("Geschenkverpackung noch nicht implementiert");
		return bs.createBestellung(bestellung, kunde);
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public List<Lieferung> findLieferungen(String nr) {
		return bs.findLieferungen(nr);
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public Lieferung createLieferung(Lieferung lieferung) {
		return bs.createLieferung(lieferung);
	}
}
