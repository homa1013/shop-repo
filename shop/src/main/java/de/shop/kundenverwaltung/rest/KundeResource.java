package de.shop.kundenverwaltung.rest;

import static de.shop.util.Constants.ADD_LINK;
import static de.shop.util.Constants.FIRST_LINK;
import static de.shop.util.Constants.KEINE_ID;
import static de.shop.util.Constants.LAST_LINK;
import static de.shop.util.Constants.LIST_LINK;
import static de.shop.util.Constants.REMOVE_LINK;
import static de.shop.util.Constants.SELF_LINK;
import static de.shop.util.Constants.UPDATE_LINK;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hibernate.validator.constraints.Email;
import org.jboss.logging.Logger;

import com.google.common.base.Strings;

import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.bestellverwaltung.rest.BestellungResource;
import de.shop.bestellverwaltung.service.BestellungService;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.domain.Adresse;
import de.shop.kundenverwaltung.domain.GeschlechtType;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.kundenverwaltung.service.KundeService.FetchType;
import de.shop.kundenverwaltung.service.KundeService.OrderByType;
import de.shop.util.interceptor.Log;
import de.shop.util.persistence.File;
import de.shop.util.rest.NotFoundException;
import de.shop.util.rest.UriHelper;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Path("/kunden")
@Produces({ APPLICATION_JSON, APPLICATION_XML + ";qs=0.75", TEXT_XML + ";qs=0.5" })
@Consumes
@Log
public class KundeResource {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	// public fuer Testklassen
	public static final String KUNDEN_ID_PATH_PARAM = "kundeId";
	public static final String KUNDEN_NACHNAME_QUERY_PARAM = "nachname";
	public static final String KUNDEN_PLZ_QUERY_PARAM = "plz";
	public static final String KUNDEN_EMAIL_QUERY_PARAM = "email";
	public static final String KUNDEN_GESCHLECHT_QUERY_PARAM = "geschlecht";
	
	private static final String NOT_FOUND_ID = "kunde.notFound.id";
	private static final String NOT_FOUND_NACHNAME = "kunde.notFound.nachname";
	private static final String NOT_FOUND_PLZ = "kunde.notFound.plz";
	private static final String NOT_FOUND_EMAIL = "kunde.notFound.email";
	private static final String NOT_FOUND_FILE = "kunde.notFound.file";

	@Context
	private UriInfo uriInfo;
    
	@Inject
	private KundeService ks;
	
	@Inject
	private BestellungService bs;

	@Inject
	private BestellungResource bestellungResource;
	
	@Inject
	private UriHelper uriHelper;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wird geloescht", this);
	}
	
	/**
	 * Mit der URL /kunden/{id} einen Kunden ermitteln
	 * @param id ID des Kunden
	 * @return Objekt mit Kundendaten, falls die ID vorhanden ist
	 */
	@GET
	@Path("{" + KUNDEN_ID_PATH_PARAM + ":[1-9][0-9]*}")
	public Response findKundeById(@PathParam(KUNDEN_ID_PATH_PARAM) Long id) {
		final AbstractKunde kunde = ks.findKundeById(id, FetchType.NUR_KUNDE);
		if (kunde == null) {
			throw new NotFoundException(NOT_FOUND_ID, id);
		}
		
		setStructuralLinks(kunde, uriInfo);
		
		return Response.ok(kunde)
				       .links(getTransitionalLinks(kunde, uriInfo))
				       .build();
	}
	
	public void setStructuralLinks(AbstractKunde kunde, UriInfo uriInfo) {
		// URI fuer Bestellungen setzen
		final URI uri = getUriBestellungen(kunde, uriInfo);
		kunde.setBestellungenUri(uri);
		
		LOGGER.trace(kunde);
	}
	
	private URI getUriBestellungen(AbstractKunde kunde, UriInfo uriInfo) {
		return uriHelper.getUri(KundeResource.class, "findBestellungenByKundeId", kunde.getId(), uriInfo);
	}
	
	public Link[] getTransitionalLinks(AbstractKunde kunde, UriInfo uriInfo) {
		final Link self = Link.fromUri(getUriKunde(kunde, uriInfo))
	                          .rel(SELF_LINK)
	                          .build();

		final Link list = Link.fromUri(uriHelper.getUri(KundeResource.class, uriInfo))
                              .rel(LIST_LINK)
                              .build();
		
		final Link add = Link.fromUri(uriHelper.getUri(KundeResource.class, uriInfo))
                             .rel(ADD_LINK)
                             .build();

		final Link update = Link.fromUri(uriHelper.getUri(KundeResource.class, uriInfo))
				                .rel(UPDATE_LINK)
				                .build();

		final Link remove = Link.fromUri(uriHelper.getUri(KundeResource.class, "deleteKunde", kunde.getId(), uriInfo))
                                .rel(REMOVE_LINK)
                                .build();

		return new Link[] { self, list, add, update, remove };
	}
	
	public URI getUriKunde(AbstractKunde kunde, UriInfo uriInfo) {
		return uriHelper.getUri(KundeResource.class, "findKundeById", kunde.getId(), uriInfo);
	}
	
	
	@GET
	@Path("/prefix/id/{id:[1-9][0-9]*}")
	@Produces({ APPLICATION_JSON, TEXT_PLAIN })
	public Collection<Long> findIdsByPrefix(@PathParam("id") String idPrefix) {
		final Collection<Long> ids = ks.findIdsByPrefix(idPrefix);
		return ids;
	}
	

	/**
	 * Mit der URL /kunden werden alle Kunden ermittelt oder
	 * mit kunden?nachname=... diejenigen mit einem bestimmten Nachnamen.
	 * @param nachname Der gemeinsame Nachname der gesuchten Kunden
	 * @return Collection mit den gefundenen Kundendaten
	 */
	@GET
	public Response findKunden(@QueryParam(KUNDEN_NACHNAME_QUERY_PARAM)
	                           @Pattern(regexp = AbstractKunde.NACHNAME_PATTERN, message = "{kunde.nachname.pattern}")
	                           String nachname,
	                           @QueryParam(KUNDEN_PLZ_QUERY_PARAM)
	                           @Pattern(regexp = "\\d{5}", message = "{adresse.plz}")
                               String plz,
                               @QueryParam(KUNDEN_EMAIL_QUERY_PARAM)
	                           @Email(message = "{kunde.email}")
                               String email,
                               @QueryParam(KUNDEN_GESCHLECHT_QUERY_PARAM)
	                           GeschlechtType geschlecht) {
		List<? extends AbstractKunde> kunden = null;
		AbstractKunde kunde = null;
		// TODO Mehrere Query-Parameter koennen angegeben sein
		if (!Strings.isNullOrEmpty(nachname)) {
			kunden = ks.findKundenByNachname(nachname, FetchType.NUR_KUNDE);
			if (kunden.isEmpty()) {
				throw new NotFoundException(NOT_FOUND_NACHNAME, nachname);
			}
		}
		else if (!Strings.isNullOrEmpty(plz)) {
			kunden = ks.findKundenByPLZ(plz);
			if (kunden.isEmpty()) {
				throw new NotFoundException(NOT_FOUND_PLZ, plz);
			}
		}
		else if (!Strings.isNullOrEmpty(email)) {
			kunde = ks.findKundeByEmail(email);
			if (kunde == null) {
				throw new NotFoundException(NOT_FOUND_EMAIL, email);
			}
		}
		else if (geschlecht != null) {
			kunden = ks.findKundenByGeschlecht(geschlecht);
			if (kunden.isEmpty()) {
				throw new NotFoundException(NOT_FOUND_EMAIL, email);
			}
		}
		else {
			kunden = ks.findAllKunden(FetchType.NUR_KUNDE, OrderByType.ID);
		}
		
		Object entity = null;
		Link[] links = null;
		if (kunden != null) {
			for (AbstractKunde k : kunden) {
				setStructuralLinks(k, uriInfo);
			}
			entity = new GenericEntity<List<? extends AbstractKunde>>(kunden){};
			links = getTransitionalLinksKunden(kunden, uriInfo);
		}
		else if (kunde != null) {
			entity = kunde;
			links = getTransitionalLinks(kunde, uriInfo);
		}
		
		return Response.ok(entity)
                       .links(links)
                       .build();
	}
	
	private Link[] getTransitionalLinksKunden(List<? extends AbstractKunde> kunden, UriInfo uriInfo) {
		if (kunden == null || kunden.isEmpty()) {
			return null;
		}
		
		final Link first = Link.fromUri(getUriKunde(kunden.get(0), uriInfo))
	                           .rel(FIRST_LINK)
	                           .build();
		final int lastPos = kunden.size() - 1;
		final Link last = Link.fromUri(getUriKunde(kunden.get(lastPos), uriInfo))
                              .rel(LAST_LINK)
                              .build();
		
		return new Link[] { first, last };
	}
	
	@GET
	@Path("/prefix/nachname/{nachname}")
	@Produces({ APPLICATION_JSON, TEXT_PLAIN })
	public Collection<String> findNachnamenByPrefix(@PathParam("nachname") String nachnamePrefix) {
		return ks.findNachnamenByPrefix(nachnamePrefix);
	}
	
	
	/**
	 * Mit der URL kunden/{id}/bestellungen die Bestellungen zu eine Kunden ermitteln
	 * @param id ID des Kunden
	 * @return Objekt mit Bestellungsdaten, falls die ID vorhanden ist
	 */
	@GET
	@Path("{id:[1-9][0-9]*}/bestellungen")
	public Response findBestellungenByKundeId(@PathParam("id") Long id) {
		final AbstractKunde kunde = ks.findKundeById(id, FetchType.NUR_KUNDE);
		if (kunde == null) {
			throw new NotFoundException(NOT_FOUND_ID, id);
		}
		final List<Bestellung> bestellungen = bs.findBestellungenByKunde(kunde,
				                                                         BestellungService.FetchType.NUR_BESTELLUNG);
		
		// URIs innerhalb der gefundenen Bestellungen anpassen
		if (bestellungen != null) {
			for (Bestellung bestellung : bestellungen) {
				bestellungResource.setStructuralLinks(bestellung, uriInfo);
			}
		}
		
		return Response.ok(new GenericEntity<List<Bestellung>>(bestellungen) {})
                       .links(getTransitionalLinksBestellungen(bestellungen, kunde, uriInfo))
                       .build();
	}
	
	private Link[] getTransitionalLinksBestellungen(List<Bestellung> bestellungen,
			                                        AbstractKunde kunde,
			                                        UriInfo uriInfo) {
		if (bestellungen == null || bestellungen.isEmpty()) {
			return new Link[0];
		}
		
		final Link self = Link.fromUri(getUriBestellungen(kunde, uriInfo))
                              .rel(SELF_LINK)
                              .build();
		
		final Link first = Link.fromUri(bestellungResource.getUriBestellung(bestellungen.get(0), uriInfo))
	                           .rel(FIRST_LINK)
	                           .build();
		
		final int lastPos = bestellungen.size() - 1;
		final Link last = Link.fromUri(bestellungResource.getUriBestellung(bestellungen.get(lastPos), uriInfo))
                              .rel(LAST_LINK)
                              .build();
		
		return new Link[] { self, first, last };
	}
	
	@GET
	@Path("{id:[1-9][0-9]*}/bestellungenIds")
	@Produces({ APPLICATION_JSON, TEXT_PLAIN + ";qs=0.75", APPLICATION_XML + ";qs=0.5" })
	public Response findBestellungenIdsByKundeId(@PathParam("id") Long kundeId) {
		final AbstractKunde kunde = ks.findKundeById(kundeId, FetchType.MIT_BESTELLUNGEN);
		if (kunde == null) {
			throw new NotFoundException(NOT_FOUND_ID, kundeId);
		}
		final Collection<Bestellung> bestellungen =
				                     bs.findBestellungenByKunde(kunde, BestellungService.FetchType.NUR_BESTELLUNG);
		
		final int anzahl = bestellungen.size();
		final Collection<Long> bestellungenIds = new ArrayList<>(anzahl);
		for (Bestellung bestellung : bestellungen) {
			bestellungenIds.add(bestellung.getId());
		}
		
		return Response.ok(new GenericEntity<Collection<Long>>(bestellungenIds) {})
			           .build();
	}
	
	/**
	 * Mit der URL /kunden/{id}/wartungsvertraege die Wartungsvertraege ermitteln
	 * zu einem bestimmten Kunden ermitteln
	 * @param id ID des Kunden
	 * @return Wartungsvertraege, falls die ID vorhanden ist
	 */
	@GET
	@Path("{id:[1-9][0-9]*}/wartungsvertraege")
	public Response findWartungsvertraegeByKundeId(@PathParam("id") Long id) {
		// Diese Methode ist bewusst NICHT implementiert, um zu zeigen,
		// wie man Methodensignaturen an der Schnittstelle fuer andere
		// Teammitglieder schon mal bereitstellt, indem einfach ein "Internal
		// Server Error (500)" produziert wird.
		// Die Kolleg/inn/en koennen nun weiterarbeiten, waehrend man selbst
		// gerade keine Zeit hat, weil andere Aufgaben Vorrang haben.
		
		// TODO findWartungsvertraegeByKundeId noch nicht implementiert
		return Response.status(INTERNAL_SERVER_ERROR)
				       .entity("findWartungsvertraegeByKundeId: NOT YET IMPLEMENTED")
				       .type(TEXT_PLAIN)
				       .build();
	}
	

	/**
	 * Mit der URL /kunden einen Privatkunden per POST anlegen.
	 * @param kunde neuer Kunde
	 * @return Response-Objekt mit URL des neuen Privatkunden
	 */
	@POST
	@Consumes({ APPLICATION_JSON, APPLICATION_XML, TEXT_XML })
	@Produces
	@Transactional
	public Response createKunde(@Valid AbstractKunde kunde) {
		kunde.setId(KEINE_ID);
		final Adresse adresse = kunde.getAdresse();
		if (adresse != null) {
			adresse.setKunde(kunde);
		}
		if (Strings.isNullOrEmpty(kunde.getPasswordWdh())) {
			// ein IT-System als REST-Client muss das Password ggf. nur 1x uebertragen
			kunde.setPasswordWdh(kunde.getPassword());
		}
		
		kunde = ks.createKunde(kunde);
		LOGGER.trace(kunde);
		
		return Response.created(getUriKunde(kunde, uriInfo))
				       .build();
	}
	
	/**
	 * Mit der URL /kunden einen Kunden per PUT aktualisieren
	 * @param kunde zu aktualisierende Daten des Kunden
	 */
	@PUT
	@Consumes({ APPLICATION_JSON, APPLICATION_XML, TEXT_XML })
	@Produces({ APPLICATION_JSON, APPLICATION_XML, TEXT_XML })
	@Transactional
	public Response updateKunde(@Valid AbstractKunde kunde) {
		// Vorhandenen Kunden ermitteln
		final AbstractKunde origKunde = ks.findKundeById(kunde.getId(), FetchType.NUR_KUNDE);
		if (origKunde == null) {
			throw new NotFoundException(NOT_FOUND_ID, kunde.getId());
		}
		LOGGER.tracef("Kunde vorher = %s", origKunde);
	
		// Daten des vorhandenen Kunden ueberschreiben
		origKunde.setValues(kunde);
		LOGGER.tracef("Kunde nachher = %s", origKunde);
		
		// Update durchfuehren
		kunde = ks.updateKunde(origKunde, false);
		setStructuralLinks(kunde, uriInfo);
		
		return Response.ok(kunde)
				       .links(getTransitionalLinks(kunde, uriInfo))
				       .build();
	}
	
	
	/**
	 * Mit der URL /kunden{id} einen Kunden per DELETE l&ouml;schen
	 * @param kundeId des zu l&ouml;schenden Kunden
	 *         gel&ouml;scht wurde, weil es zur gegebenen id keinen Kunden gibt
	 */
	@Path("{id:[1-9][0-9]*}")
	@DELETE
	@Produces
	@Transactional
	public void deleteKunde(@PathParam("id") long kundeId) {
		ks.deleteKundeById(kundeId);
	}

	
	@Path("{id:[1-9][0-9]*}/file")
	@POST
	@Consumes({ "image/jpeg", "image/pjpeg", "image/png" })  // RESTEasy unterstuetzt nicht video/mp4
	@Transactional
	public Response upload(@PathParam("id") Long kundeId, byte[] bytes) {
		ks.setFile(kundeId, bytes);
		return Response.created(uriHelper.getUri(KundeResource.class, "download", kundeId, uriInfo))
				       .build();
	}
	
	@Path("{id:[1-9][0-9]*}/file")
	@GET
	@Produces({ "image/jpeg", "image/pjpeg", "image/png" })
	@Transactional  // Nachladen der Datei : AbstractKunde referenziert File mit Lazy Fetching
	public byte[] download(@PathParam("id") Long kundeId) {
		final AbstractKunde kunde = ks.findKundeById(kundeId, FetchType.NUR_KUNDE);
		if (kunde == null) {
			throw new NotFoundException(NOT_FOUND_ID, kundeId);
		}
		
		final File file = kunde.getFile();
		if (file == null) {
			throw new NotFoundException(NOT_FOUND_FILE, kundeId);
		}
		LOGGER.tracef("%s", file.toString());
		
		return file.getBytes();
	}
}
