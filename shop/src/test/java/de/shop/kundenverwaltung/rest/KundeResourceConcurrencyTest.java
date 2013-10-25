package de.shop.kundenverwaltung.rest;

import static de.shop.util.TestConstants.KUNDEN_ID_URI;
import static de.shop.util.TestConstants.KUNDEN_URI;
import static de.shop.util.TestConstants.PASSWORD;
import static de.shop.util.TestConstants.PASSWORD_ADMIN;
import static de.shop.util.TestConstants.USERNAME;
import static de.shop.util.TestConstants.USERNAME_ADMIN;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.fest.assertions.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.util.AbstractResourceTest;
import de.shop.util.HttpsConcurrencyHelper;


//Logging durch java.util.logging
/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@RunWith(Arquillian.class)
public class KundeResourceConcurrencyTest extends AbstractResourceTest {
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
	
	private static final long TIMEOUT = 5;

	private static final Long KUNDE_ID_UPDATE = Long.valueOf(120);
	private static final String NEUER_NACHNAME = "Testname";
	private static final String NEUER_NACHNAME_2 = "Neuername";
	private static final Long KUNDE_ID_DELETE1 = Long.valueOf(122);
	private static final Long KUNDE_ID_DELETE2 = Long.valueOf(124);

	@Test
	@InSequence(1)
	public void updateUpdate() throws InterruptedException, ExecutionException, TimeoutException {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_UPDATE;
    	final String neuerNachname = NEUER_NACHNAME;
    	final String neuerNachname2 = NEUER_NACHNAME_2;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                            .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();

    	final AbstractKunde kunde = response.readEntity(AbstractKunde.class);

    	// Konkurrierendes Update
		// Aus den gelesenen JSON-Werten ein neues JSON-Objekt mit neuem Nachnamen bauen
		kunde.setNachname(neuerNachname2);
		
		final Callable<Integer> concurrentUpdate = new Callable<Integer>() {
			@Override
			public Integer call() {
				final Response response = new HttpsConcurrencyHelper()
				                          .getHttpsClient(USERNAME, PASSWORD)
                                          .target(KUNDEN_URI)
                                          .request()
                                          .accept(APPLICATION_JSON)
                                          .put(json(kunde));
				final int status = response.getStatus();
				response.close();
				return Integer.valueOf(status);
			}
		};
    	final Integer status = Executors.newSingleThreadExecutor()
    			                        .submit(concurrentUpdate)
    			                        .get(TIMEOUT, SECONDS);   // Warten bis der "parallele" Thread fertig ist
		assertThat(status.intValue()).isEqualTo(HTTP_OK);
		
    	// Fehlschlagendes Update
		// Aus den gelesenen JSON-Werten ein neues JSON-Objekt mit neuem Nachnamen bauen
		kunde.setNachname(neuerNachname);
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                      .request()
                                                      .accept(APPLICATION_JSON)
                                                      .put(json(kunde));
	    	
		// Then
		assertThat(response.getStatus()).isEqualTo(HTTP_CONFLICT);
		response.close();
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(2)
	public void updateDelete() throws InterruptedException, ExecutionException, TimeoutException {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_DELETE1;
    	final String neuerNachname = NEUER_NACHNAME;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                             .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                             .request()
                                             .accept(APPLICATION_JSON)
                                             .get();

		final AbstractKunde kunde = response.readEntity(AbstractKunde.class);

		// Konkurrierendes Delete
    	final Callable<Integer> concurrentDelete = new Callable<Integer>() {
			@Override
			public Integer call() {
				final Response response = new HttpsConcurrencyHelper()
				                          .getHttpsClient(USERNAME_ADMIN, PASSWORD_ADMIN)
                                          .target(KUNDEN_ID_URI)
                                          .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                          .request()
                                          .delete();
				final int status = response.getStatus();
				response.close();
				return Integer.valueOf(status);
			}
		};
    	final Integer status = Executors.newSingleThreadExecutor()
    			                        .submit(concurrentDelete)
    			                        .get(TIMEOUT, SECONDS);   // Warten bis der "parallele" Thread fertig ist
		assertThat(status.intValue()).isEqualTo(HTTP_NO_CONTENT);
		
    	// Fehlschlagendes Update
		kunde.setNachname(neuerNachname);
		response = getHttpsClient(USERNAME, PASSWORD).target(KUNDEN_URI)
                                                     .request()
                                                     .accept(APPLICATION_JSON)
                                                     .put(json(kunde));
			
		// Then
    	assertThat(response.getStatus()).isEqualTo(HTTP_NOT_FOUND);
    	response.close();
		
		LOGGER.finer("ENDE");
	}
	
	@Test
	@InSequence(3)
	public void deleteUpdate() throws InterruptedException, ExecutionException, TimeoutException {
		LOGGER.finer("BEGINN");
		
		// Given
		final Long kundeId = KUNDE_ID_DELETE2;
    	final String neuerNachname = NEUER_NACHNAME;
		
		// When
		Response response = getHttpsClient().target(KUNDEN_ID_URI)
                                            .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM, kundeId)
                                            .request()
                                            .accept(APPLICATION_JSON)
                                            .get();

		final AbstractKunde kunde = response.readEntity(AbstractKunde.class);

		// Konkurrierendes Update
		kunde.setNachname(neuerNachname);
    	final Callable<Integer> concurrentUpdate = new Callable<Integer>() {
			@Override
			public Integer call() {
				final Response response = new HttpsConcurrencyHelper()
				                          .getHttpsClient(USERNAME, PASSWORD)
                                          .target(KUNDEN_URI)
                                          .request()
                                          .accept(APPLICATION_JSON)
                                          .put(json(kunde));
				final int status = response.getStatus();
				response.close();
				return Integer.valueOf(status);
			}
		};
    	final Integer status = Executors.newSingleThreadExecutor()
    			                        .submit(concurrentUpdate)
    			                        .get(TIMEOUT, SECONDS);   // Warten bis der "parallele" Thread fertig ist
		assertThat(status.intValue()).isEqualTo(HTTP_OK);
		
    	// Erfolgreiches Delete trotz konkurrierendem Update
		response = getHttpsClient(USERNAME_ADMIN, PASSWORD_ADMIN).target(KUNDEN_ID_URI)
                                                                 .resolveTemplate(KundeResource.KUNDEN_ID_PATH_PARAM,
                                                                		          kundeId)
                                                                 .request()
                                                                 .delete();
			
		// Then
    	assertThat(response.getStatus()).isEqualTo(HTTP_NO_CONTENT);
    	response.close();
		
		LOGGER.finer("ENDE");
	}
}
