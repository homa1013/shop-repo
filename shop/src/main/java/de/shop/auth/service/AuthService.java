package de.shop.auth.service;

import static de.shop.kundenverwaltung.domain.AbstractKunde.FIND_USERNAME_BY_USERNAME_PREFIX;
import static de.shop.kundenverwaltung.domain.AbstractKunde.PARAM_USERNAME_PREFIX;
import static de.shop.util.Constants.HASH_ALGORITHM;
import static de.shop.util.Constants.HASH_CHARSET;
import static de.shop.util.Constants.HASH_ENCODING;
import static de.shop.util.Constants.SECURITY_DOMAIN;
import static org.jboss.security.auth.spi.Util.createPasswordHash;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.SimpleGroup;

import com.google.common.base.Strings;

import de.shop.auth.domain.RolleType;
import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.service.KundeService;
import de.shop.kundenverwaltung.service.KundeService.FetchType;
import de.shop.util.interceptor.Log;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@ApplicationScoped
@Log
public class AuthService implements Serializable {
	private static final long serialVersionUID = -2736040689592627172L;
	
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	private static final String LOCALHOST = "localhost";
	private static final int MANAGEMENT_PORT = 9990;  // JBossAS hatte den Management-Port 9999
	
	@Inject
	private transient EntityManager em;
	
	@Inject
	private KundeService ks;
	
	@PostConstruct
	private void postConstruct() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde erzeugt", this);
	}
	
	@PreDestroy
	private void preDestroy() {
		LOGGER.debugf("CDI-faehiges Bean %s wurde geloescht", this);
	}


	/**
	 * In Anlehnung an org.jboss.test.PasswordHasher von Scott Stark
	 * @param password zu verschluesselndes Password
	 * @return Verschluesseltes Password, d.h. der berechnete Hashwert
	 */
	public String verschluesseln(String password) {
		if (password == null) {
			return null;
		}
		
		// Alternativ:
		// org.jboss.crypto.CryptoUtil.createPasswordHash
		return createPasswordHash(HASH_ALGORITHM, HASH_ENCODING, HASH_CHARSET, null, password);
	}
	
	/**
	 * Ueberpruefung des Passworts eines Kunden
	 * @param kunde Zu ueberpruefender Kunde
	 * @param passwort Das abzugleichende Passwort
	 * @return true, falls das abzugleichende Passwort mit dem des Kunden uebereinstimmt. falls sonst.
	 */
	public boolean validatePassword(AbstractKunde kunde, String passwort) {
		if (kunde == null) {
			return false;
		}
		
		final String verschluesselt = verschluesseln(passwort);
		return verschluesselt.equals(kunde.getPassword());
	}
	
	/**
	 * Zu einem Kunden neue Rollen hinzufuegen
	 * @param kundeId ID des betroffenen Kunden
	 * @param rollen Neue Rollen
	 */
	public void addRollen(Long kundeId, Collection<RolleType> rollen) {
		if (rollen == null || rollen.isEmpty()) {
			return;
		}

		ks.findKundeById(kundeId, FetchType.NUR_KUNDE)
		  .addRollen(rollen);
		flushSecurityCache(kundeId.toString());
	}

	/**
	 * Von einem Kunden Rollen wegnehmen
	 * @param kundeId ID des betroffenen Kunden
	 * @param rollen Die wegzunehmenden Rollen
	 */
	public void removeRollen(Long kundeId, Collection<RolleType> rollen) {
		if (rollen == null || rollen.isEmpty()) {
			return;
		}

		ks.findKundeById(kundeId, FetchType.NUR_KUNDE)
		  .removeRollen(rollen);
		flushSecurityCache(kundeId.toString());
	}
	
	/*
	 * siehe http://community.jboss.org/thread/169263
	 * siehe https://docs.jboss.org/author/display/AS7/Management+Clients
	 * siehe https://github.com/jbossas/jboss-as/blob/master/controller-client/src/main/java/org/jboss/as/controller/client/ModelControllerClient.java
	 * siehe http://community.jboss.org/wiki/FormatOfADetypedOperationRequest
	 * siehe http://community.jboss.org/wiki/DetypedDescriptionOfTheAS7ManagementModel
	 * 
	 * Gleicher Ablauf mit CLI (= command line interface):
	 * cd %JBOSS_HOME%\bin
	 * jboss-cli.bat -c --command=/subsystem=security/security-domain=shop:flush-cache(principal=myUserName)
	 */
	private static void flushSecurityCache(String username) {
		// ModelControllerClient ist abgeleitet vom Interface Autoclosable
		try (ModelControllerClient client = ModelControllerClient.Factory.create(LOCALHOST, MANAGEMENT_PORT)) {
			final ModelNode address = new ModelNode();
			address.add("subsystem", "security");
			address.add("security-domain", SECURITY_DOMAIN);

			final ModelNode operation = new ModelNode();
			operation.get("address").set(address);
			operation.get("operation").set("flush-cache");
			operation.get("principal").set(username);

			final ModelNode result = client.execute(operation);
			final String resultString = result.get("outcome").asString();
			if (!"success".equals(resultString)) {
				throw new IllegalStateException("FEHLER bei der Operation \"flush-cache\" fuer den Security-Cache: "
						                        + resultString);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Rollen zum eingeloggten User ermitteln
	 * @return Liste der Rollen des eingeloggten Users
	 */
	public List<RolleType> getEigeneRollen() {		
		final List<RolleType> rollen = new LinkedList<>();
		
		// Authentifiziertes Subject ermitteln
		Subject subject = null;
		try {
			subject = Subject.class.cast(PolicyContext.getContext("javax.security.auth.Subject.container"));
		}
		catch (PolicyContextException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		if (subject == null) {
			return null;
		}

		// Gruppe "Roles" ermitteln
		final Set<Principal> principals = subject.getPrincipals(Principal.class);
		for (Principal p : principals) {
			if (!(p instanceof SimpleGroup)) {
				continue;
			}

			final SimpleGroup sg = SimpleGroup.class.cast(p);
			if (!"Roles".equals(sg.getName())) {
				continue;
			}
			
			// Rollen ermitteln
			final Enumeration<Principal> members = sg.members();
			while (members.hasMoreElements()) {
				final String rolle = members.nextElement().toString();
				if (rolle != null) {
					rollen.add(RolleType.valueOf(rolle.toUpperCase(Locale.getDefault())));
				}
			}
		}
		return rollen;
	}

	/**
	 * Zu einem Praefix alle passenden Usernamen ermitteln
	 * @param usernamePrefix Gemeinsamer Praefix fuer potenzielle Usernamen 
	 * @return Liste der potenziellen Usernamen
	 */
	public List<String> findUsernameListByUsernamePrefix(String usernamePrefix) {
		return em.createNamedQuery(FIND_USERNAME_BY_USERNAME_PREFIX, String.class)
				 .setParameter(PARAM_USERNAME_PREFIX, usernamePrefix + '%')
				 .getResultList();
	}
	

	public static void main(String[] args) throws IOException {
		for (;;) {
			System.out.print("Password (Abbruch durch <Return>): ");
			final BufferedReader reader = new BufferedReader(
					                          new InputStreamReader(System.in, Charset.defaultCharset()));
			final String password = reader.readLine();
			if (Strings.isNullOrEmpty(password)) {
				break;
			}
			final String passwordHash = createPasswordHash(HASH_ALGORITHM, HASH_ENCODING, HASH_CHARSET, null, password);
			System.out.println("Verschluesselt: " + passwordHash + System.getProperty("line.separator"));
		}
		
		System.out.println("FERTIG");
	}
}
