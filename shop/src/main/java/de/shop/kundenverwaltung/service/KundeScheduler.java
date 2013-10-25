package de.shop.kundenverwaltung.service;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.logging.Logger;

import de.shop.auth.domain.RolleType;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.util.interceptor.Log;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Stateless
@Log
public class KundeScheduler {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	@Inject
	private transient EntityManager em;
	
	@Inject
	private KundeService ks;
	
	@Schedule(dayOfMonth = "*", hour = "2", minute = "0", year = "*", persistent = false)
	public void deleteKundeOhneBestellungen() {
		final List<AbstractKunde> kunden =
				                  em.createNamedQuery(AbstractKunde.FIND_KUNDEN_OHNE_BESTELLUNGEN, AbstractKunde.class)
				                    .getResultList();
		for (AbstractKunde k : kunden) {
			final Collection<RolleType> rollen = k.getRollen();
			if (rollen != null && rollen.contains(RolleType.ADMIN)) {
				// Admin nicht loeschen
				continue;
			}
			ks.deleteKunde(k);
			LOGGER.infof("Kunde #%d wurde geloescht (ohne Bestellungen)", k.getId());
		}
	}
}
