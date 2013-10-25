package de.shop.bestellverwaltung.service;

import java.util.List;

import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Lieferung;
import de.shop.kundenverwaltung.domain.AbstractKunde;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
public interface BestellungService {
	public enum FetchType {
		NUR_BESTELLUNG,
		MIT_LIEFERUNGEN
	}

	/**
	 * Bestellung zu gegebener ID suchen
	 * @param id Gegebene ID
	 * @param fetch Welche Objekte sollen mitgeladen werden, z.B. Lieferungen
	 * @return Die gefundene Bestellung oder null
	 */
	Bestellung findBestellungById(Long id, FetchType fetch);
	
	/**
	 * Bestellungen zu einem gegebenen Kunden suchen
	 * @param kunde Der gegebene Kunde
	 * @param fetch Welche Objekte sollen mitgeladen werden, z.B. Lieferungen
	 * @return Die gefundenen Bestellungen
	 */
	List<Bestellung> findBestellungenByKunde(AbstractKunde kunde, FetchType fetch);
	
	/**
	 * Den Kunden zu einer gegebenen Bestellung-ID suchen
	 * @param id Bestellung-ID
	 * @return Der gefundene Kunde oder null
	 */
	AbstractKunde findKundeById(Long id);
	
	/**
	 * Zuordnung einer neuen, transienten Bestellung zu einem existierenden, persistenten Kunden,
	 * identifiziert durch den Username.
	 * @param bestellung Die neue Bestellung
	 * @param username Der Benutzername des zuzuordnenden Kunden
	 * @return Die neue Bestellung einschliesslich generierter ID
	 */
	Bestellung createBestellung(Bestellung bestellung, String username);
	
	/**
	 * Zuordnung einer neuen, transienten Bestellung zu einem existierenden, persistenten Kunden.
	 * Der Kunde ist fuer den EntityManager bekannt, die Bestellung dagegen nicht. Das Zusammenbauen
	 * wird sowohl fuer einen Web Service aus auch fuer eine Webanwendung benoetigt.
	 * @param bestellung Die neue Bestellung
	 * @param kunde Der existierende Kunde
	 * @return Die neue Bestellung einschliesslich generierter ID
	 */
	Bestellung createBestellung(Bestellung bestellung, AbstractKunde kunde);
	
	/**
	 * Lieferungen zu gegebenem Praefix der Liefernummer suchen
	 * @param nr Praefix der Liefernummer
	 * @return Liste der Lieferungen
	 */
	List<Lieferung> findLieferungen(String nr);
	
	/**
	 * Eine neue Lieferung in der DB anlegen
	 * @param lieferung Die neue Lieferung
	 * @return Die neue Lieferung einschliesslich generierter ID
	 */
	Lieferung createLieferung(Lieferung lieferung);
}
