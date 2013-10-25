package de.shop.bestellverwaltung.service;

import static de.shop.util.Constants.KEINE_ID;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.jboss.logging.Logger;

import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Lieferung;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.util.interceptor.Log;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Log
public class BestellungServiceImpl implements Serializable, BestellungService {
	private static final long serialVersionUID = 3365404106904200415L;
	
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	@Inject
	private transient EntityManager em;
	
	@Inject
	private KundeService ks;
	
	@Inject
	@NeueBestellung
	private transient Event<Bestellung> event;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung findBestellungById(Long id, FetchType fetch) {
		Bestellung bestellung = null;
		if (fetch == null || FetchType.NUR_BESTELLUNG.equals(fetch)) {
			bestellung = em.find(Bestellung.class, id);
		}
		else if (FetchType.MIT_LIEFERUNGEN.equals(fetch)) {
			try {
			bestellung = em.createNamedQuery(Bestellung.FIND_BESTELLUNG_BY_ID_FETCH_LIEFERUNGEN, Bestellung.class)
					       .setParameter(Bestellung.PARAM_ID, id)
					       .getSingleResult();
			}
			catch (NoResultException e) {
				return null;
			}
		}
		return bestellung;
	}

	/**
	 * {inheritDoc}
	 */
	@Override
	public List<Bestellung> findBestellungenByKunde(AbstractKunde kunde, FetchType fetch) {
		if (kunde == null) {
			return Collections.emptyList();
		}
		
		List<Bestellung> bestellungen;
		switch (fetch) {
			case NUR_BESTELLUNG:
				bestellungen = em.createNamedQuery(Bestellung.FIND_BESTELLUNGEN_BY_KUNDEID, Bestellung.class)
	                             .setParameter(Bestellung.PARAM_KUNDEID, kunde.getId())
	                             .getResultList();
				break;
			case MIT_LIEFERUNGEN:
				bestellungen = em.createNamedQuery(Bestellung.FIND_BESTELLUNGEN_BY_KUNDEID_FETCH_LIEFERUNGEN,
                                                   Bestellung.class)
                                 .setParameter(Bestellung.PARAM_KUNDEID, kunde.getId())
                                 .getResultList();
				break;
			default:
				bestellungen = Collections.emptyList();
		}
		return bestellungen;
	}

	
	/**
	 * {inheritDoc}
	 */
	@Override
	public AbstractKunde findKundeById(Long id) {
		try {
			return em.createNamedQuery(Bestellung.FIND_KUNDE_BY_ID, AbstractKunde.class)
					 .setParameter(Bestellung.PARAM_ID, id)
					 .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}



	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung createBestellung(Bestellung bestellung, String username) {
		if (bestellung == null) {
			return null;
		}

		// Den persistenten Kunden mit der transienten Bestellung verknuepfen
		final AbstractKunde kunde = ks.findKundeByUserName(username);
		return createBestellung(bestellung, kunde);
	}
	
	/**
	 * {inheritDoc}
	 */
	@Override
	public Bestellung createBestellung(Bestellung bestellung, AbstractKunde kunde) {
		if (bestellung == null || kunde == null) {
			return null;
		}

		// Den persistenten Kunden mit der transienten Bestellung verknuepfen
		if (!em.contains(kunde)) {
			kunde = ks.findKundeById(kunde.getId(), KundeService.FetchType.MIT_BESTELLUNGEN);
		}
		bestellung.setKunde(kunde);
		kunde.addBestellung(bestellung);
		
		// Vor dem Abspeichern IDs zuruecksetzen:
		// IDs koennten einen Wert != null haben, wenn sie durch einen Web Service uebertragen wurden
		bestellung.setId(KEINE_ID);
		for (Bestellposition bp : bestellung.getBestellpositionen()) {
			bp.setId(KEINE_ID);
		}
		
		em.persist(bestellung);
		event.fire(bestellung);
		
		return bestellung;
	}

	
	/**
	 * {inheritDoc}
	 */
	@Override
	public List<Lieferung> findLieferungen(String nr) {
		return em.createNamedQuery(Lieferung.FIND_LIEFERUNGEN_BY_LIEFERNR_FETCH_BESTELLUNGEN, Lieferung.class)
                 .setParameter(Lieferung.PARAM_LIEFER_NR, nr)
                 .getResultList();
	}
	
	/**
	 * {inheritDoc}
	 */
	@Override
	public Lieferung createLieferung(Lieferung lieferung) {
		if (lieferung == null) {
			return null;
		}
		
		lieferung.setId(KEINE_ID);
		em.persist(lieferung);
		return lieferung;
	}
}
