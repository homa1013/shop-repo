package de.shop.kundenverwaltung.service;

import static de.shop.util.Constants.MAX_AUTOCOMPLETE;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.jboss.logging.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import de.shop.auth.domain.RolleType;
import de.shop.auth.service.AuthService;
import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellposition_;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.domain.Bestellung_;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.domain.AbstractKunde_;
import de.shop.kundenverwaltung.domain.GeschlechtType;
import de.shop.kundenverwaltung.domain.Privatkunde;
import de.shop.kundenverwaltung.domain.Wartungsvertrag;
import de.shop.util.NoMimeTypeException;
import de.shop.util.interceptor.Log;
import de.shop.util.persistence.ConcurrentDeletedException;
import de.shop.util.persistence.File;
import de.shop.util.persistence.FileHelper;
import de.shop.util.persistence.MimeType;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Log
public class KundeService implements Serializable {
	private static final long serialVersionUID = 5654417703891549367L;
	
	public enum FetchType {
		NUR_KUNDE,
		MIT_BESTELLUNGEN,
		MIT_WARTUNGSVERTRAEGEN
	}
	
	public enum OrderByType {
		UNORDERED,
		ID
	}
	
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	// genau 1 Eintrag mit 100 % Fuellgrad
	private static final Map<String, Object> GRAPH_BESTELLUNGEN = new HashMap<>(1, 1);
	private static final Map<String, Object> GRAPH_WARTUNGSVERTRAEGE = new HashMap<>(1, 1); 
	
	static {
		GRAPH_BESTELLUNGEN.put("javax.persistence.loadgraph", AbstractKunde.GRAPH_BESTELLUNGEN);
		GRAPH_WARTUNGSVERTRAEGE.put("javax.persistence.loadgraph", AbstractKunde.GRAPH_WARTUNGSVERTRAEGE);
	}
	
	@Inject
	private transient EntityManager em;
	
	@Inject
	private AuthService authService;
	
	@Inject
	private FileHelper fileHelper;
	
	@Inject
	private transient ManagedExecutorService managedExecutorService;

	@Inject
	@NeuerKunde
	private transient Event<AbstractKunde> event;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}

	/**
	 * Suche nach einem Kunden anhand der ID
	 * @param id ID des gesuchten Kunden
	 * @param fetch Angabe, welche Objekte mitgeladen werden sollen
	 * @return Der gesuchte Kunde oder null, falls es keinen zur gegebenen ID gibt
	 */
	public AbstractKunde findKundeById(Long id, FetchType fetch) {
		if (id == null) {
			return null;
		}
		
		AbstractKunde kunde;
		switch (fetch) {
			case NUR_KUNDE:
				kunde = em.find(AbstractKunde.class, id);
				break;
			
			case MIT_BESTELLUNGEN:
				try {
					kunde = em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN, AbstractKunde.class)
					          .setParameter(AbstractKunde.PARAM_KUNDE_ID, id)
                              .getSingleResult();
				}
				catch (NoResultException e) {
					kunde = null;
				}
				// FIXME https://hibernate.atlassian.net/browse/HHH-8285 : @NamedEntityGraph ab Java EE 7 bzw. JPA 2.1
				//kunde = em.find(AbstractKunde.class, id, GRAPH_BESTELLUNGEN);
				break;
				
			case MIT_WARTUNGSVERTRAEGEN:
				kunde = em.find(AbstractKunde.class, id, GRAPH_WARTUNGSVERTRAEGE);
				break;

			default:
				kunde = em.find(AbstractKunde.class, id);
				break;
		}
		return kunde;
	}

	
	/**
	 * Potenzielle IDs zu einem gegebenen ID-Praefix suchen
	 * @param idPrefix der Praefix zu potenziellen IDs als String
	 * @return Liste der passenden Praefixe
	 */
	public List<Long> findIdsByPrefix(String idPrefix) {
		if (Strings.isNullOrEmpty(idPrefix)) {
			return Collections.emptyList();
		}
		final List<Long> ids = em.createNamedQuery(AbstractKunde.FIND_IDS_BY_PREFIX, Long.class)
				                 .setParameter(AbstractKunde.PARAM_KUNDE_ID_PREFIX, idPrefix + '%')
				                 .getResultList();
		return ids;
	}
	
	/**
	 * Kunden suchen, deren ID den gleiche Praefix hat.
	 * @param id Praefix der ID
	 * @return Liste mit Kunden mit passender ID
	 */
	public List<AbstractKunde> findKundenByIdPrefix(Long id) {
		if (id == null) {
			return Collections.emptyList();
		}
		
		return em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_ID_PREFIX, AbstractKunde.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_ID_PREFIX, id.toString() + '%')
				 .setMaxResults(MAX_AUTOCOMPLETE)
				 .getResultList();
	}
	
	/**
	 * Alle Kunden in einer bestimmten Reihenfolge ermitteln
	 * @param fetch Angabe, welche Objekte mitgeladen werden sollen, z.B. Bestellungen.
	 * @param order Sortierreihenfolge, z.B. nach aufsteigenden IDs.
	 * @return Liste der Kunden
	 */
	public List<AbstractKunde> findAllKunden(FetchType fetch, OrderByType order) {
		final TypedQuery<AbstractKunde> query = OrderByType.ID.equals(order)
				                        ? em.createNamedQuery(AbstractKunde.FIND_KUNDEN_ORDER_BY_ID,
										                      AbstractKunde.class)
				                        : em.createNamedQuery(AbstractKunde.FIND_KUNDEN, AbstractKunde.class);
		switch (fetch) {
			case NUR_KUNDE:
				break;
			case MIT_BESTELLUNGEN:
				query.setHint("javax.persistence.loadgraph", AbstractKunde.GRAPH_BESTELLUNGEN);
				break;
			case MIT_WARTUNGSVERTRAEGEN:
				query.setHint("javax.persistence.loadgraph", AbstractKunde.GRAPH_WARTUNGSVERTRAEGE);
				break;
			default:
				break;
		}
		
		final List<AbstractKunde> kunden = query.getResultList();
		return kunden;
	}
	

	/**
	 * Kunden mit gleichem Nachnamen suchen.
	 * @param nachname Der gemeinsame Nachname der gesuchten Kunden
	 * @param fetch Angabe, welche Objekte mitgeladen werden sollen, z.B. Bestellungen
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenByNachname(String nachname, FetchType fetch) {
		List<AbstractKunde> kunden;
		switch (fetch) {
			case NUR_KUNDE:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME, AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;
			
			case MIT_BESTELLUNGEN:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN,
						                     AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;

			default:
				kunden = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME, AbstractKunde.class)
						   .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname)
                           .getResultList();
				break;
		}
		
		// FIXME https://hibernate.atlassian.net/browse/HHH-8285 : @NamedEntityGraph ab Java EE 7 bzw. JPA 2.1
		//final TypedQuery<AbstractKunde> query = em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_NACHNAME,
		//                                                            AbstractKunde.class)
		//				                          .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME, nachname);
		//switch (fetch) {
		//	case NUR_KUNDE:
		//		break;
		//	case MIT_BESTELLUNGEN:
		//		query.setHint("javax.persistence.loadgraph", AbstractKunde.GRAPH_BESTELLUNGEN);
		//		break;
		//	case MIT_WARTUNGSVERTRAEGEN:
		//		query.setHint("javax.persistence.loadgraph", AbstractKunde.GRAPH_WARTUNGSVERTRAEGE);
		//		break;
		//	default:
		//		break;
		//}
		//
		//final List<AbstractKunde> kunden = query.getResultList();
		return kunden;
	}

	
	/**
	 * Suche nach Nachnamen mit dem gleichen Praefix
	 * @param nachnamePrefix der gemeinsame Praefix fuer die potenziellen Nachnamen 
	 * @return Liste der Nachnamen mit gleichem Praefix
	 */
	public List<String> findNachnamenByPrefix(String nachnamePrefix) {
		return em.createNamedQuery(AbstractKunde.FIND_NACHNAMEN_BY_PREFIX, String.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_NACHNAME_PREFIX, nachnamePrefix + '%')
				 .setMaxResults(MAX_AUTOCOMPLETE)
				 .getResultList();
	}

	/**
	 * Den Kunden zu einer gegebenen Emailadresse suchen.
	 * @param email Die gegebene Emailadresse
	 * @return Der gefundene Kunde oder null, falls es keinen solchen Kunden gibt
	 */
	public AbstractKunde findKundeByEmail(String email) {
		AbstractKunde kunde;
		try {
			kunde = em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_EMAIL, AbstractKunde.class)
					  .setParameter(AbstractKunde.PARAM_KUNDE_EMAIL, email)
					  .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
		
		return kunde;
	}


	/**
	 * Kunden mit gleicher Postleitzahl suchen
	 * @param plz Die gegebene Postleitzahl
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenByPLZ(String plz) {
		return em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_PLZ, AbstractKunde.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_ADRESSE_PLZ, plz)
				 .getResultList();
	}
	
	/**
	 * Kunden suchen, die seit einem bestimmten Datum Kunde sind.
	 * @param seit Das Datum
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenBySeit(Date seit) {
		return em.createNamedQuery(AbstractKunde.FIND_KUNDEN_BY_DATE, AbstractKunde.class)
				 .setParameter(AbstractKunde.PARAM_KUNDE_SEIT, seit)
				 .getResultList();
	}
	
	/**
	 * Kunden mit gleichem Geschlecht suchen.
	 * @param geschlecht Das Geschlecht
	 * @return Liste der gefundenen Kunden
	 */
	public List<Privatkunde> findKundenByGeschlecht(GeschlechtType geschlecht) {
		return em.createNamedQuery(Privatkunde.FIND_BY_GESCHLECHT, Privatkunde.class)
				 .setParameter(Privatkunde.PARAM_GESCHLECHT, geschlecht)
				 .getResultList();
	}
	
	/**
	 * Alle Privat- und Firmenkunden suchen.
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findPrivatkundenFirmenkunden() {
		return em.createNamedQuery(AbstractKunde.FIND_PRIVATKUNDEN_FIRMENKUNDEN, AbstractKunde.class)
				 .getResultList();
	}

	
	/**
	 * Den Kunden zu einem Benutzernamen suchen.
	 * @param userName Der Benutzername, zu dem der passende Kunde gesucht wird.
	 * @return Der gefundene Kunde oder null, falls es zum angegebenen Benutzernamen keinen Kunden gibt.
	 */
	public AbstractKunde findKundeByUserName(String userName) {
		try {
			return em.createNamedQuery(AbstractKunde.FIND_KUNDE_BY_USERNAME, AbstractKunde.class)
					 .setParameter(AbstractKunde.PARAM_KUNDE_USERNAME, userName)
					 .getSingleResult();
		}
		catch (NoResultException e) {
			return null;
		}
	}
	
	/**
	 * Kunden mit gleichem Nachnamen suchen und dabei eine Criteria-Query verwenden.
	 * @param nachname Der Nachname der gesuchten Kunden
	 * @return Liste der gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenByNachnameCriteria(String nachname) {
		// SELECT k
		// FROM   AbstractKunde k
		// WHERE  k.nachname = ?
				
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<AbstractKunde> criteriaQuery = builder.createQuery(AbstractKunde.class);
		final Root<AbstractKunde> k = criteriaQuery.from(AbstractKunde.class);

		final Path<String> nachnamePath = k.get(AbstractKunde_.nachname);
		
		final Predicate pred = builder.equal(nachnamePath, nachname);
		criteriaQuery.where(pred);

		final List<AbstractKunde> kunden = em.createQuery(criteriaQuery).getResultList();
		return kunden;
	}

	/**
	 * Kunden mit einer Mindestbestellmenge suchen
	 * @param minMenge Die minimale Anzahl bestellter Artikel
	 * @return Die gefundenen Kunden
	 */
	public List<AbstractKunde> findKundenMitMinBestMenge(short minMenge) {
		// SELECT DISTINCT k
		// FROM   AbstractKunde k
		//        JOIN k.bestellungen b
		//        JOIN b.bestellpositionen bp
		// WHERE  bp.anzahl >= ?
		
		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<AbstractKunde> criteriaQuery  = builder.createQuery(AbstractKunde.class);
		final Root<AbstractKunde> k = criteriaQuery.from(AbstractKunde.class);

		final Join<AbstractKunde, Bestellung> b = k.join(AbstractKunde_.bestellungen);
		final Join<Bestellung, Bestellposition> bp = b.join(Bestellung_.bestellpositionen);
		criteriaQuery.where(builder.gt(bp.<Short>get(Bestellposition_.anzahl), minMenge))
		             .distinct(true);
		
		return em.createQuery(criteriaQuery).getResultList();
	}
	
	/**
	 * Einen neuen Kunden anlegen
	 * @param kunde Der neue Kunde
	 * @return Der neue Kunde einschliesslich generierter ID
	 */
	public <T extends AbstractKunde> T createKunde(T kunde) {
		if (kunde == null) {
			return kunde;
		}
	
		// Pruefung, ob ein solcher Kunde schon existiert
		final AbstractKunde tmp = findKundeByEmail(kunde.getEmail());
		if (tmp != null) {
			throw new EmailExistsException(kunde.getEmail());
		}
		
		// Password verschluesseln
		passwordVerschluesseln(kunde);
		
		// Rolle setzen
		kunde.addRollen(Sets.newHashSet(RolleType.KUNDE));
	
		em.persist(kunde);
		event.fire(kunde);
		
		return kunde;
	}


	/**
	 * Einen vorhandenen Kunden aktualisieren
	 * @param kunde Der aktualisierte Kunde
	 * @param geaendertPassword Wurde das Passwort aktualisiert und muss es deshalb verschluesselt werden?
	 * @return Der aktualisierte Kunde
	 */
	public <T extends AbstractKunde> T updateKunde(T kunde, boolean geaendertPassword) {
		if (kunde == null) {
			return null;
		}
		
		// kunde vom EntityManager trennen, weil anschliessend z.B. nach Id und Email gesucht wird
		em.detach(kunde);
		
		// Wurde das Objekt konkurrierend geloescht?
		AbstractKunde tmp = findKundeById(kunde.getId(), FetchType.NUR_KUNDE);
		if (tmp == null) {
			throw new ConcurrentDeletedException(kunde.getId());
		}
		em.detach(tmp);
		
		// Gibt es ein anderes Objekt mit gleicher Email-Adresse?
		tmp = findKundeByEmail(kunde.getEmail());
		if (tmp != null) {
			em.detach(tmp);
			if (tmp.getId().longValue() != kunde.getId().longValue()) {
				// anderes Objekt mit gleichem Attributwert fuer email
				throw new EmailExistsException(kunde.getEmail());
			}
		}
		
		// Password verschluesseln
		if (geaendertPassword) {
			passwordVerschluesseln(kunde);
		}

		kunde = em.merge(kunde);   // OptimisticLockException
		kunde.setPasswordWdh(kunde.getPassword());
		
		return kunde;
	}

	/**
	 * Einen Kunden in der DB loeschen.
	 * @param kunde Der zu loeschende Kunde
	 */
	public void deleteKunde(AbstractKunde kunde) {
		if (kunde == null) {
			return;
		}

		deleteKundeById(kunde.getId());
	}

	/**
	 * Einen Kunden zu gegebener ID loeschen
	 * @param kundeId Die ID des zu loeschenden Kunden
	 */
	public void deleteKundeById(Long kundeId) {
		final AbstractKunde kunde = findKundeById(kundeId, FetchType.MIT_BESTELLUNGEN);
		if (kunde == null) {
			// Der Kunde existiert nicht oder ist bereits geloescht
			return;
		}

		final boolean hasBestellungen = hasBestellungen(kunde);
		if (hasBestellungen) {
			throw new KundeDeleteBestellungException(kunde);
		}

		// Kundendaten loeschen
		em.remove(kunde);
	}

	
	/**
	 * Einem Kunden eine hochgeladene Datei ohne MIME Type (bei RESTful WS) zuordnen
	 * @param kundeId Die ID des Kunden
	 * @param bytes Das Byte-Array der hochgeladenen Datei
	 */
	public AbstractKunde setFile(Long kundeId, byte[] bytes) {
		final AbstractKunde kunde = findKundeById(kundeId, FetchType.NUR_KUNDE);
		if (kunde == null) {
			return null;
		}
		final MimeType mimeType = fileHelper.getMimeType(bytes);
		setFile(kunde, bytes, mimeType);
		return kunde;
	}
	
	/**
	 * Einem Kunden eine hochgeladene Datei zuordnen
	 * @param kunde Der betroffene Kunde
	 * @param bytes Das Byte-Array der hochgeladenen Datei
	 * @param mimeTypeStr Der MIME-Type als String
	 */
	public AbstractKunde setFile(AbstractKunde kunde, byte[] bytes, String mimeTypeStr) {
		final MimeType mimeType = MimeType.build(mimeTypeStr);
		setFile(kunde, bytes, mimeType);
		return kunde;
	}
	
	private void setFile(AbstractKunde kunde, byte[] bytes, MimeType mimeType) {
		if (mimeType == null) {
			throw new NoMimeTypeException();
		}
		
		final String filename = fileHelper.getFilename(kunde.getClass(), kunde.getId(), mimeType);
		
		// Gibt es noch kein (Multimedia-) File
		File file = kunde.getFile();
		if (kunde.getFile() == null) {
			file = new File(bytes, filename, mimeType);
			LOGGER.tracef("Neue Datei %s", file);
			kunde.setFile(file);
			em.persist(file);
		}
		else {
			file.set(bytes, filename, mimeType);
			LOGGER.tracef("Ueberschreiben der Datei %s", file);
			em.merge(file);
		}

		// Hochgeladenes Bild/Video/Audio in einem parallelen Thread als Datei fuer die Web-Anwendung abspeichern
		final File newFile = kunde.getFile();
		final Runnable storeFile = new Runnable() {
			@Override
			public void run() {
				fileHelper.store(newFile);
			}
		};
		managedExecutorService.execute(storeFile);
	}
	
	/**
	 * Die Wartungsvertraege eines Kunden ermitteln
	 * @param kundeId Die ID des Kunden
	 * @return Die Liste mit seinen Wartungsvertraegen
	 */
	public List<Wartungsvertrag> findWartungsvertraege(Long kundeId) {
		return em.createNamedQuery(Wartungsvertrag.FIND_WARTUNGSVERTRAEGE_BY_KUNDE_ID, Wartungsvertrag.class)
				 .setParameter(Wartungsvertrag.PARAM_KUNDE_ID, kundeId)
				 .getResultList();
	}

	
	/**
	 * Einen neuen Wartungsvertrag anlegen
	 * @param wartungsvertrag Der neue Wartungsvertrag
	 * @param kunde Der zugehoerige Kunde
	 * @return Der neue Wartungsvertrag einschliesslich ggf. generierter ID
	 */
	public Wartungsvertrag createWartungsvertrag(Wartungsvertrag wartungsvertrag, AbstractKunde kunde) {
		if (wartungsvertrag == null) {
			return null;
		}
		
		findKundeById(kunde.getId(), FetchType.NUR_KUNDE);
		em.persist(wartungsvertrag);
		return wartungsvertrag;
	}
	

	private static boolean hasBestellungen(AbstractKunde kunde) {
		LOGGER.debugf("hasBestellungen BEGINN: %s", kunde);
		
		boolean result = false;
		
		// Gibt es den Kunden und hat er mehr als eine Bestellung?
		// Bestellungen nachladen wegen Hibernate-Caching
		if (kunde != null && kunde.getBestellungen() != null && !kunde.getBestellungen().isEmpty()) {
			result = true;
		}
		
		LOGGER.debugf("hasBestellungen ENDE: %s", result);
		return result;
	}

	
	private void passwordVerschluesseln(AbstractKunde kunde) {
		LOGGER.debugf("passwordVerschluesseln BEGINN: %s", kunde);

		final String unverschluesselt = kunde.getPassword();
		final String verschluesselt = authService.verschluesseln(unverschluesselt);
		kunde.setPassword(verschluesselt);
		kunde.setPasswordWdh(verschluesselt);

		LOGGER.debugf("passwordVerschluesseln ENDE: %s", verschluesselt);
	}
}
