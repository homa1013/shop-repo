package de.shop.kundenverwaltung.service;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jboss.logging.Logger;

import de.shop.kundenverwaltung.domain.AbstractKunde;
import de.shop.kundenverwaltung.domain.Adresse;
import de.shop.util.interceptor.Log;
import de.shop.util.mail.AbsenderMail;
import de.shop.util.mail.AbsenderName;
import de.shop.util.mail.EmpfaengerMail;
import de.shop.util.mail.EmpfaengerName;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@ApplicationScoped
@Log
public class KundeObserver {
	private static final String NEWLINE = System.getProperty("line.separator");
	
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	@Inject
	private transient Session session;
	
	@Inject
	@AbsenderMail
	private String absenderMail;
	
	@Inject
	@AbsenderName
	private String absenderName;

	@Inject
	@EmpfaengerMail
	private String empfaengerMail;
	
	@Inject
	@EmpfaengerName
	private String empfaengerName;
	
	@Inject
	private transient ManagedExecutorService managedExecutorService;
	
	@PostConstruct
	// Attribute mit @Inject sind initialisiert
	private void postConstruct() {
		if (absenderMail == null || empfaengerMail == null) {
			LOGGER.warn("Absender oder Empfaenger fuer Markteting-Emails sind nicht gesetzt.");
			return;
		}
		LOGGER.infof("Absender fuer Markteting-Emails: %s <%s>", absenderName, absenderMail);
		LOGGER.infof("Empfaenger fuer Markteting-Emails: %s <%s>", empfaengerName, empfaengerMail);
	}
	
	// Loose Kopplung durch @Observes, d.h. ohne JMS
	public void onCreateKunde(@Observes @NeuerKunde final AbstractKunde kunde) {
		if (absenderMail == null || empfaengerMail == null || kunde == null) {
			return;
		}
		
		final Runnable sendMail = new Runnable() {
			@Override
			public void run() {
				final MimeMessage message = new MimeMessage(session);

				try {
					// Absender setzen
					final InternetAddress absenderObj = new InternetAddress(absenderMail, absenderName);
					message.setFrom(absenderObj);
					
					// Empfaenger setzen
					final InternetAddress empfaenger = new InternetAddress(empfaengerMail, empfaengerName);
					message.setRecipient(RecipientType.TO, empfaenger);   // RecipientType: TO, CC, BCC

					final Adresse adr = kunde.getAdresse();
					
					// Subject setzen
					final String subject = adr == null
							               ? "Neuer Kunde ohne Adresse"
							               : "Neuer Kunde in " + adr.getPlz() + " " + adr.getOrt();
					message.setSubject(subject);
					
					// HTML-Text setzen mit MIME Type "text/html"
					final String text = adr == null
							            ? "<p><b>" + kunde.getVorname() + " " + kunde.getNachname()
							            + "</b></p>" + NEWLINE
							            : "<p><b>" + kunde.getVorname() + " " + kunde.getNachname()
							            + "</b></p>" + NEWLINE
					                    + "<p>" + adr.getPlz() + " " + adr.getOrt() + "</p>" + NEWLINE
					                    + "<p>" + adr.getStrasse() + " " + adr.getHausnr() + "</p>" + NEWLINE;

					message.setContent(text, "text/html");
					
					// Hohe Prioritaet einstellen
					//message.setHeader("Importance", "high");
					//message.setHeader("Priority", "urgent");
					//message.setHeader("X-Priority", "1");
					
					// HTML-Text mit einem Bild als Attachment
					Transport.send(message);
				}
				catch (MessagingException | UnsupportedEncodingException e) {
					LOGGER.error(e.getMessage());
				}
			}
		};
		managedExecutorService.execute(sendMail);
		
		//final Future<?> future = managedExecutorService.submit(sendMail);
		//LOGGER.debugf("future: %s", future);
	}
}
