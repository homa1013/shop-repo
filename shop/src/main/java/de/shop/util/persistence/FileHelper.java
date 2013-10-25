package de.shop.util.persistence;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

import de.shop.util.interceptor.Log;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@ApplicationScoped
@Named
public class FileHelper implements Serializable {
	private static final long serialVersionUID = 12904207356717310L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass());
	
	// Zulaessige Extensionen fuer Upload mit einer Webseite
	private String extensionen;
	
	// Verzeichnis fuer hochgeladene Dateien
	private transient Path path;

	@PostConstruct
	private void postConstruct() {
		// Bei .flv wird der Mime-Type weder bei RichFaces noch bei RESTEasy erkannt
		extensionen = "jpg, jpeg, png, mp4, wav";
		LOGGER.infof("Extensionen fuer Datei-Upload: %s", extensionen);
		
		String appName = null;
		Context ctx = null;
		try {
			ctx = new InitialContext();  // InitialContext implementiert nicht das Interface Autoclosable
			appName = String.class.cast(ctx.lookup("java:app/AppName"));				
		}
		catch (NamingException e) {
			throw new RuntimeException(e);
		}
		finally {
			if (ctx != null) {
					try {
						ctx.close();
					}
					catch (NamingException e) {
						LOGGER.warn(e.getMessage(), e);
					}
			}
		}
		
		// Verzeichnis im Dateisystem des Betriebssystems
		path = Paths.get(System.getenv("JBOSS_HOME"), "standalone", "deployments", "filesDb.war", appName);
				
		if (Files.exists(path)) {
			LOGGER.infof("Verzeichnis fuer hochgeladene Dateien: %s", path);
		}
		else {
			LOGGER.errorf("Kein Verzeichnis %s fuer hochgeladene Dateien vorhanden", path);
		}
	}

	/**
	 * MIME-Type zu einer Datei als byte[] ermitteln
	 * @param bytes Byte-Array, zu dem der MIME-Type ermitelt wird
	 * @return Der zugehoerige MIME-Type
	 */
	public MimeType getMimeType(byte[] bytes) {
		if (bytes == null) {
			return null;
		}

		try (final InputStream inputStream = new ByteArrayInputStream(bytes)) {
			final String mimeTypeStr = URLConnection.guessContentTypeFromStream(inputStream);
			
			LOGGER.tracef("MIME-Type: %s", mimeTypeStr);
			return MimeType.build(mimeTypeStr);
		}
		catch (IOException e) {
			LOGGER.warn("Fehler beim Ermitteln des MIME-Types");
			return null;
		}
	}
	
	public String getFilename(Class<? extends Serializable> clazz, Object id, MimeType mimeType) {
		final String filename = clazz.getSimpleName() + "_" + id + "." + mimeType.getExtension();
		LOGGER.tracef("Dateiname: %s", filename);
		return filename;
	}
	
	public String getExtensionen() {
		return extensionen;
	}
	
	@Log
	public void store(File file) {
		if (file == null) {
			return;
		}
		
		final String filename = file.getFilename();
		final Path absoluteFilename = path.resolve(filename);
		LOGGER.tracef("Absoluter Dateiname: %s", absoluteFilename);
		
		// aktuelle Datei nicht ueberschreiben?
		if (Files.exists(absoluteFilename)) {
			long creationTime = 0L;
			try {
				creationTime = Files.getFileAttributeView(absoluteFilename, BasicFileAttributeView.class)
				                    .readAttributes()
									.creationTime()
									.toMillis();
			}
			catch (IOException e) {
				LOGGER.warnf(e, "Fehler beim Lesen des Erzeugungsdatums der Datei %s", absoluteFilename);
			}
			
			// Die Datei wurde beim Hochladen evtl. in einem parallelen Thread angelegt,
			// der evtl. vor dem Abspeichern der Verwaltungsdaten in der DB fertig war.
			// Als Zeitunterschied bzw. Toleranz sollten 1000 Millisekunden ausreichend sein.
			if (creationTime + 1000 > file.getAktualisiert().getTime()) {
				LOGGER.tracef("Die Datei %s existiert bereits", filename);
				return;
			}
		}
		
		// byte[] als Datei abspeichern
		try (final InputStream inputStream = new ByteArrayInputStream(file.getBytes())) {
			Files.copy(inputStream, absoluteFilename, REPLACE_EXISTING);
		}
		catch (IOException e) {
			LOGGER.warnf(e, "Fehler beim Speichern der Datei %s", absoluteFilename);
		}
	}
}
