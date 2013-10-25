package de.shop.kundenverwaltung.rest;

import static de.shop.util.Constants.FIRST_LINK;
import static de.shop.util.Constants.LAST_LINK;
import static de.shop.util.Constants.SELF_LINK;
import static de.shop.util.TestConstants.ARTIKEL_URI;
import static de.shop.util.TestConstants.BESTELLUNGEN_URI;
import static de.shop.util.TestConstants.KUNDEN_ID_FILE_URI;
import static de.shop.util.TestConstants.KUNDEN_ID_URI;
import static de.shop.util.TestConstants.KUNDEN_URI;
import static de.shop.util.TestConstants.PASSWORD;
import static de.shop.util.TestConstants.PASSWORD_ADMIN;
import static de.shop.util.TestConstants.PASSWORD_FALSCH;
import static de.shop.util.TestConstants.USERNAME;
import static de.shop.util.TestConstants.USERNAME_ADMIN;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNSUPPORTED_TYPE;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.GERMAN;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.assertions.api.Assertions.filter;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.resteasy.api.validation.ResteasyConstraintViolation;
import org.jboss.resteasy.api.validation.ViolationReport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.shop.auth.domain.RolleType;
import de.shop.bestellverwaltung.domain.Bestellposition;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.domain.Adresse;
import de.shop.kundenverwaltung.domain.GeschlechtType;
import de.shop.kundenverwaltung.domain.Privatkunde;
import de.shop.util.AbstractResourceTest;


//Logging durch java.util.logging
/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@RunWith(Arquillian.class)
public class KundeResourceTest extends AbstractResourceTest {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
	
	private static final Long KUNDE_ID_VORHANDEN_MIT_BESTELLUNGEN = Long.valueOf(101);
	private static final Long KUNDE_ID_NICHT_VORHANDEN = Long.valueOf(1000);
	private static final Long KUNDE_ID_UPDATE = Long.valueOf(120);
	private static final Long KUNDE_ID_DELETE = Long.valueOf(122);
	private static final Long KUNDE_ID_DELETE_MIT_BESTELLUNGEN = Long.valueOf(101);
	private static final Long KUNDE_ID_DELETE_FORBIDDEN = Long.valueOf(101);
	private static final String NACHNAME_VORHANDEN = "Alpha";
	private static final String NACHNAME_NICHT_VORHANDEN = "Falschername";
	private static final String NACHNAME_INVALID = "Test9";
	private static final String NEUER_NACHNAME = "Nachnameneu";
	private static final String NEUER_NACHNAME_INVALID = "!";
	private static final String NEUER_VORNAME = "Vorname";
	private static final String NEUE_EMAIL = NEUER_NACHNAME + "@test.de";
	private static final String NEUE_EMAIL_INVALID = "?";
	private static final short NEUE_KATEGORIE = 1;
	private static final BigDecimal NEUER_RABATT = new BigDecimal("0.15");
	private static final BigDecimal NEUER_UMSATZ = new BigDecimal(10_000_000);
	private static final Date NEU_SEIT = new GregorianCalendar(2000, 0, 31).getTime();
	private static final String NEUE_PLZ = "76133";
	private static final String NEUE_PLZ_FALSCH = "1234";
	private static final String NEUER_ORT = "Karlsruhe";
	private static final String NEUE_STRASSE = "Testweg";
	private static final String NEUE_HAUSNR = "1";
	private static final String NEUES_PASSWORD = "neuesPassword";
	private static final Long ARTIKEL_ID_VORHANDEN = Long.valueOf(300);
	
	private static final String IMAGE_FILENAME = "image.png";
	private static final String IMAGE_PATH_UPLOAD = "src/test/resources/rest/" + IMAGE_FILENAME;
	private static final String IMAGE_MIMETYPE = "image/png";
	private static final String IMAGE_PATH_DOWNLOAD = "target/" + IMAGE_FILENAME;
	private static final Long KUNDE_ID_UPLOAD = Long.valueOf(102);

	private static final String IMAGE_INVALID = "image.bmp";
	private static final String IMAGE_INVALID_PATH = "src/test/resources/rest/" + IMAGE_INVALID;
	private static final String IMAGE_INVALID_MIMETYPE = "image/bmp";
	
	
	@Test
	@InSequence(1)
	public void validate() {
		assertThat(true).isTrue();
	}
	
	@Ignore
	@Test
	@InSequence(2)
	public void beispielIgnore() {
		assertThat(true).isFalse();
	}
	
	@Ignore
	@Test
	@InSequence(3)
	public void beispielFailMitIgnore() {
		fail("Beispiel fuer fail()");
	}
	
	@Test
	@InSequence(10)
	public void findKundeMitBestellungenById() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_VORHANDEN_MIT_BESTELLUNGEN;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                            .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();
	
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		final AbstractKunde kunde = response.readEntity(AbstractKunde.class);
		assertThat(kunde.getId()).isEqualTo(kundeId);
		assertThat(kunde.getNachname()).isNotEmpty();
		assertThat(kunde.getAdresse()).isNotNull();
		assertThat(kunde.isAgbAkzeptiert()).isTrue();
		
		// Link-Header fuer Bestellungen pruefen
		assertThat(response.getLinks()).isNotEmpty();
		assertThat(response.getLink(SELF_LINK).getUri().toString()).contains(String.valueOf(kundeId));
		
		final URI bestellungenUri = kunde.getBestellungenUri();
		assertThat(bestellungenUri).isNotNull();
		
		response = getHttpsClient().target(bestellungenUri)
				                   .request()
				                   .accept(APPLICATION_JSON)
				                   .get();
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		
		// Verweist der Link-Header der ermittelten Bestellungen auf den Kunden?
		final Collection<Bestellung> bestellungen = response.readEntity(new GenericType<Collection<Bestellung>>() { });
		
		assertThat(bestellungen).isNotEmpty()
		                        .doesNotContainNull()
		                        .doesNotHaveDuplicates();
		for (Bestellung b : bestellungen) {
			assertThat(b.getKundeUri().toString()).endsWith(String.valueOf(kundeId));			
		}
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(11)
	public void findKundeByIdNichtVorhanden() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_NICHT_VORHANDEN;
		
		// When
		final Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                                  .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                                  .request()
                                                  .acceptLanguage(GERMAN)
                                                  .get();

    	// Then
    	assertThat(response.getStatus()).isEqualTo(HTTP_NOT_FOUND);
    	final String fehlermeldung = response.readEntity(String.class);
    	assertThat(fehlermeldung).startsWith("Kein Kunde mit der ID")
    	                         .endsWith("gefunden.");
		
		LOGGER.finer("ENDE");
	}

	@Test
	@InSequence(20)
	public void findKundenByNachnameVorhanden() {
		LOGGER.finer("BEGINN");
		
		// Given
		final String nachname = NACHNAME_VORHANDEN;

		// When
		Response response = getHttpsClient().target(KUNDEN_URI)
                                            .queryParam(KundeResource.KUNDEN_NACHNAME_QUERY_PARAM, nachname)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();

		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		
		final Collection<AbstractKunde> kunden =
				                        response.readEntity(new GenericType<Collection<AbstractKunde>>() { });
		assertThat(kunden).isNotEmpty()
		                  .doesNotContainNull()
		                  .doesNotHaveDuplicates();
		
		assertThat(response.getLinks()).isNotEmpty();
		assertThat(response.getLink(FIRST_LINK)).isNotNull();
		assertThat(response.getLink(LAST_LINK)).isNotNull();

		for (AbstractKunde k : kunden) {
			assertThat(k.getNachname()).isEqualTo(nachname);
			
			final URI bestellungenUri = k.getBestellungenUri();
			assertThat(bestellungenUri).isNotNull();
			response = getHttpsClient().target(bestellungenUri)
					                   .request()
					                   .accept(APPLICATION_JSON)
					                   .get();
			assertThat(response.getStatus()).isIn(HTTP_OK, HTTP_NOT_FOUND);
			response.close();           // readEntity() wurde nicht aufgerufen
		}
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(21)
	public void findKundenByNachnameNichtVorhanden() {
		LOGGER.finer("BEGINN");
		
		// Given
		final String nachname = NACHNAME_NICHT_VORHANDEN;
		
		// When
		final Response response = getHttpsClient().target(KUNDEN_URI)
                                                  .queryParam(KundeResource.KUNDEN_NACHNAME_QUERY_PARAM, nachname)
                                                  .request()
                                                  .acceptLanguage(GERMAN)
                                                  .get();
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_NOT_FOUND);
		final String fehlermeldung = response.readEntity(String.class);
		assertThat(fehlermeldung).isEqualTo("Kein Kunde mit dem Nachnamen \"" + nachname + "\" gefunden.");

		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(22)
	public void findKundenByNachnameInvalid() {
		LOGGER.finer("BEGINN");
		
		// Given
		final String nachname = NACHNAME_INVALID;
		
		// When
		final Response response = getHttpsClient().target(KUNDEN_URI)
                                                  .queryParam(KundeResource.KUNDEN_NACHNAME_QUERY_PARAM, nachname)
                                                  .request()
                                                  .accept(APPLICATION_JSON)
                                                  .acceptLanguage(ENGLISH)
                                                  .get();
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_BAD_REQUEST);
		assertThat(response.getHeaderString("validation-exception")).isEqualTo("true");
		final ViolationReport violationReport = response.readEntity(ViolationReport.class);
		final List<ResteasyConstraintViolation> violations = violationReport.getParameterViolations();
		assertThat(violations).isNotEmpty();
		
		final ResteasyConstraintViolation violation =
				                          filter(violations).with("message")
                                                            .equalsTo("A lastname must start with exactly one capital letter followed by at least one lower letter, and composed names with \"-\" are allowed.")
                                                            .get()
                                                            .iterator()
                                                            .next();
		assertThat(violation.getValue()).isEqualTo(String.valueOf(nachname));

		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(30)
	public void findKundenByGeschlecht() {
		LOGGER.finer("BEGINN");
		
		for (GeschlechtType geschlecht : GeschlechtType.values()) {
			// When
			final Response response = getHttpsClient().target(KUNDEN_URI)
                                                      .queryParam(KundeResource.KUNDEN_GESCHLECHT_QUERY_PARAM,
                                                    		      geschlecht)
                                                      .request()
                                                      .accept(APPLICATION_JSON)
                                                      .get();
			final Collection<Privatkunde> kunden = response.readEntity(new GenericType<Collection<Privatkunde>>() { });
			
			// Then
            assertThat(kunden).isNotEmpty()             // siehe Testdaten
                              .doesNotContainNull()
                              .doesNotHaveDuplicates();
            for (Privatkunde k : kunden) {
    			assertThat(k.getGeschlecht()).isEqualTo(geschlecht);
            }
		}
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(40)
	public void createPrivatkunde() throws URISyntaxException {
		LOGGER.finer("BEGINN");
		
		// Given
		final String nachname = NEUER_NACHNAME;
		final String vorname = NEUER_VORNAME;
		final String email = NEUE_EMAIL;
		final short kategorie = NEUE_KATEGORIE;
		final BigDecimal rabatt = NEUER_RABATT;
		final BigDecimal umsatz = NEUER_UMSATZ;
		final Date seit = NEU_SEIT;
		final boolean agbAkzeptiert = true;
		final String plz = NEUE_PLZ;
		final String ort = NEUER_ORT;
		final String strasse = NEUE_STRASSE;
		final String hausnr = NEUE_HAUSNR;
		final String neuesPassword = NEUES_PASSWORD;
		
		final Privatkunde kunde = new Privatkunde(nachname, vorname, email, seit);
		kunde.setVorname(vorname);
		kunde.setKategorie(kategorie);
		kunde.setRabatt(rabatt);
		kunde.setUmsatz(umsatz);
		kunde.setAgbAkzeptiert(agbAkzeptiert);
		final Adresse adresse = new Adresse(plz, ort, strasse, hausnr);
		kunde.setAdresse(adresse);
		kunde.setPassword(neuesPassword);
		kunde.setPasswordWdh(neuesPassword);
		kunde.addRollen(Arrays.asList(RolleType.KUNDE, RolleType.MITARBEITER));
		
		Response response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                              .request()
                                                              .post(json(kunde));
			
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_CREATED);
		String location = response.getLocation().toString();
		response.close();
		
		final int startPos = location.lastIndexOf('/');
		final String idStr = location.substring(startPos + 1);
		final Long id = Long.valueOf(idStr);
		assertThat(id).isPositive();
		
		// Einloggen als neuer Kunde und Bestellung aufgeben

		// Given (2)
		final Long artikelId = ARTIKEL_ID_VORHANDEN;
		final String username = idStr;

		// When (2)
		final Bestellung bestellung = new Bestellung();
		final Bestellposition bp = new Bestellposition();
		bp.setArtikelUri(new URI(ARTIKEL_URI + "/" + artikelId));
		bp.setAnzahl((short) 1);
		bestellung.addBestellposition(bp);
		
		// Then (2)
		response = getHttpsClient(username, neuesPassword).target(BESTELLUNGEN_URI)
                                                          .request()
                                                          .post(json(bestellung));

		assertThat(response.getStatus()).isEqualTo(HTTP_CREATED);
		location = response.getLocation().toString();
		response.close();
		assertThat(location).isNotEmpty();

		LOGGER.finer("ENDE");
	}
	
	
	@Test
	@InSequence(41)
	public void createPrivatkundeInvalid() {
		LOGGER.finer("BEGINN");
		
		// Given
		final String nachname = NEUER_NACHNAME_INVALID;
		final String vorname = NEUER_VORNAME;
		final String email = NEUE_EMAIL_INVALID;
		final Date seit = NEU_SEIT;
		final boolean agbAkzeptiert = false;
		final String password = NEUES_PASSWORD;
		final String passwordWdh = NEUES_PASSWORD + "x";
		final String plz = NEUE_PLZ_FALSCH;
		final String ort = NEUER_ORT;
		final String strasse = NEUE_STRASSE;
		final String hausnr = NEUE_HAUSNR;

		final Privatkunde kunde = new Privatkunde(nachname, vorname, email, seit);
		kunde.setVorname(vorname);
		kunde.setAgbAkzeptiert(agbAkzeptiert);
		kunde.setPassword(password);
		kunde.setPasswordWdh(passwordWdh);
		final Adresse adresse = new Adresse(plz, ort, strasse, hausnr);
		adresse.setKunde(kunde);
		kunde.setAdresse(adresse);
		
		// When
		final Response response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                                    .request()
                                                                    .accept(APPLICATION_JSON)
                                                                    // engl. Fehlermeldungen ohne Umlaute ;-)
                                                                    .acceptLanguage(ENGLISH)
                                                                    .post(json(kunde));
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_BAD_REQUEST);
		assertThat(response.getHeaderString("validation-exception")).isEqualTo("true");
		final ViolationReport violationReport = response.readEntity(ViolationReport.class);
		response.close();
		
		final List<ResteasyConstraintViolation> violations = violationReport.getParameterViolations();
		assertThat(violations).isNotEmpty();
		
		ResteasyConstraintViolation violation =
				                    filter(violations).with("message")
                                                      .equalsTo("A lastname must have at least 2 and may only have up to 32 characters.")
                                                      .get()
                                                      .iterator()
                                                      .next();
		assertThat(violation.getValue()).isEqualTo(String.valueOf(nachname));
		
		violation = filter(violations).with("message")
                                      .equalsTo("A lastname must start with exactly one capital letter followed by at least one lower letter, and composed names with \"-\" are allowed.")
                                      .get()
                                      .iterator()
                                      .next();
		assertThat(violation.getValue()).isEqualTo(String.valueOf(nachname));

		violation = filter(violations).with("message")
				                      .equalsTo("The email address is invalid.")
				                      .get()
				                      .iterator()
				                      .next();
		assertThat(violation.getValue()).isEqualTo(email);
		
		
		violation = filter(violations).with("message")
                                      .equalsTo("Passwords are not equal.")
                                      .get()
                                      .iterator()
                                      .next();
		// @ScriptAssert steht bei der Klasse und nicht bei einem Attribut:
		// violation.getValue() ruft toString() auf dem Objekt der Klasse Privatkunde auf
		assertThat(violation.getValue()).contains(password).contains(passwordWdh);
		
		violation = filter(violations).with("message")
                                      .equalsTo("The terms were not accepted.")
                                      .get()
                                      .iterator()
                                      .next();
		assertThat(violation.getValue()).isEqualTo(String.valueOf(agbAkzeptiert));
		
		
		violation = filter(violations).with("message")
                                      .equalsTo("The ZIP code doesn't have 5 digits.")
                                      .get()
                                      .iterator()
                                      .next();
		assertThat(violation.getValue()).isEqualTo(plz);
		
		LOGGER.finer("ENDE");
	}

	
	@Test
	@InSequence(42)
	public void createPrivatkundeFalschesPassword() {
		LOGGER.finer("BEGINN");
		
		// Given
		// Bei falschem Passwort muss der Inhalt des JSON-Datensatzes egal sein
		final AbstractKunde kunde = new AbstractKunde() {
			private static final long serialVersionUID = 1L;
		};
		
		// When
		final Response response = getHttpsClient(USERNAME, PASSWORD_FALSCH).target(KUNDEN_URI)
                                                                           .request()
                                                                           .post(json(kunde));
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_UNAUTHORIZED);
		response.close();
		
		LOGGER.finer("ENDE");
	}
	
	
	@Test
	@InSequence(50)
	public void updateKunde() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_UPDATE;
		final String neuerNachname = NEUER_NACHNAME;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                            .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();
		AbstractKunde kunde = response.readEntity(AbstractKunde.class);
		assertThat(kunde.getId()).isEqualTo(kundeId);
		final int origVersion = kunde.getVersion();
    	
    	// Aus den gelesenen JSON-Werten ein neues JSON-Objekt mit neuem Nachnamen bauen
		kunde.setNachname(neuerNachname);
    	
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                     .request()
                                                     .accept(APPLICATION_JSON)
                                                     .put(json(kunde));
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		kunde = response.readEntity(AbstractKunde.class);
		assertThat(kunde.getVersion()).isGreaterThan(origVersion);
		
		// Erneutes Update funktioniert, da die Versionsnr. aktualisiert ist
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                     .request()
                                                     .put(json(kunde));
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		response.close();
		
		// Erneutes Update funktioniert NICHT, da die Versionsnr. NICHT aktualisiert ist
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                     .request()
                                                     .put(json(kunde));
		assertThat(response.getStatus()).isEqualTo(HTTP_CONFLICT);
		response.close();
		
		LOGGER.finer("ENDE");
   	}
	
	@Test
	@InSequence(60)
	public void deleteKunde() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_DELETE;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                            .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();
		assertThat(response.getStatus()).isEqualTo(HTTP_OK);
		response.close();
		
		response = getHttpsClient(USERNAME_ADMIN, PASSWORD_ADMIN).target(KUNDEN_ID_URI)
                                                                 .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM,
                                                                		          kundeId)
                                                                 .request()
                                                                 .delete();
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
		response.close();
		
		response = getHttpsClient().target(KUNDEN_ID_URI)
                                   .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                   .request()
                                   .accept(APPLICATION_JSON)
                                   .get();
       	assertThat(response.getStatus()).isEqualTo(HTTP_NOT_FOUND);
		response.close();
        
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(61)
	public void deleteKundeMitBestellung() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_DELETE_MIT_BESTELLUNGEN;
		
		// When
		final Response response =
				       getHttpsClient(USERNAME_ADMIN, PASSWORD_ADMIN).target(KUNDEN_ID_URI)
                                                                   .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM,
                                                                                    kundeId)
                                                                   .request()
                                                                   .acceptLanguage(GERMAN)
                                                                   .delete();
		
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_BAD_REQUEST);
		final String fehlermeldung = response.readEntity(String.class);
		assertThat(fehlermeldung).startsWith("Der Kunde mit ID")
		                         .endsWith("Bestellung(en).");
		
		LOGGER.finer("ENDE");
	}
	
	
	@Test
	@InSequence(62)
	public void deleteKundeFehlendeBerechtigung() {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_DELETE_FORBIDDEN;
		
		// When
		final Response response =
                       getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_ID_URI)
                                                         .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                                         .request()
                                                         .delete();
		
		// Then
		assertThat(response.getStatus()).isIn(HTTP_FORBIDDEN, HTTP_NOT_FOUND);
		response.close();
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(70)
	public void uploadDownload() throws IOException {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_UPLOAD;
		final String path = IMAGE_PATH_UPLOAD;
		final String mimeType = IMAGE_MIMETYPE;
		
		// Datei einlesen
		final byte[] uploadBytes = Files.readAllBytes(Paths.get(path));
		
		// When
		Response response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_ID_FILE_URI)
                                                              .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM,
                                                            		           kundeId)
                                                              .request()
                                                              .post(entity(uploadBytes, mimeType));

		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_CREATED);
		// id extrahieren aus http://localhost:8080/shop/rest/kunden/<id>/file
		final String location = response.getLocation().toString();
		response.close();
		
		final String idStr = location.replace(KUNDEN_URI + '/', "")
				                     .replace("/file", "");
		assertThat(idStr).isEqualTo(kundeId.toString());
		
		// When (2)
		// Download der zuvor hochgeladenen Datei
		byte[] downloadBytes;
		
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_ID_FILE_URI)
                                                     .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                                     .request()
                                                     .accept(mimeType)
                                                     .get();
		downloadBytes = response.readEntity(new GenericType<byte[]>() {});
		
		// Then (2)
		assertThat(uploadBytes.length).isEqualTo(downloadBytes.length);
		assertThat(uploadBytes).isEqualTo(downloadBytes);
		
		// Abspeichern des heruntergeladenen byte[] als Datei im Unterverz. target zur manuellen Inspektion
		Files.write(Paths.get(IMAGE_PATH_DOWNLOAD), downloadBytes);
		LOGGER.info("Heruntergeladene Datei abgespeichert: " + IMAGE_PATH_DOWNLOAD);
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(71)
	public void uploadInvalidMimeType() throws IOException {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_UPLOAD;
		final String path = IMAGE_INVALID_PATH;
		final String mimeType = IMAGE_INVALID_MIMETYPE;
		
		// Datei einlesen
		final byte[] uploadBytes = Files.readAllBytes(Paths.get(path));
		
		// When
		final Response response =
				       getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_ID_FILE_URI)
                                                         .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                                         .request()
                                                         .post(entity(uploadBytes, mimeType));
		
		assertThat(response.getStatus()).isEqualTo(HTTP_UNSUPPORTED_TYPE);
		response.close();
	}
}
