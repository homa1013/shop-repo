package de.shop.util;

import static de.shop.util.TestConstants.TEST_CLASSES;
import static de.shop.util.TestConstants.WEB_PROJEKT;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
public enum ArchiveBuilder {
	INSTANCE;
	
	static final String TEST_WAR = WEB_PROJEKT + ".war";
	
	private static final String CLASSES_DIR = "target/classes";
	private static final String WEBAPP_DIR = "src/main/webapp";
	
	private final WebArchive archive = ShrinkWrap.create(WebArchive.class, TEST_WAR);
	private final WebArchive archiveMitTestklassen = ShrinkWrap.create(WebArchive.class, TEST_WAR);

	/**
	 */
	private ArchiveBuilder() {
		addManifestMf();
		addWebInfWebseiten();		
		addJars();
		addKlassen();

		archiveMitTestklassen.merge(archive);
		addTestKlassen();
	}

	private void addManifestMf() {
		// Zeilen in META-INF\Manifest.mf duerfen laut Java-Spezifikation nur 69 Zeichen breit sein
		// Umgebrochene Zeilen muessen mit einem Leerzeichen beginnen
		archive.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                                           + "Dependencies: org.jboss.jts services export,org.jboss.as.controller-cl\n"
				                           + " ient,org.jboss.dmr\n"));
		
	}
	
	private void addWebInfWebseiten() {
		final JavaArchive tmp = ShrinkWrap.create(JavaArchive.class);
		// XML-Konfigurationsdateien und Webseiten als JAR einlesen
		tmp.as(ExplodedImporter.class).importDirectory(WEBAPP_DIR);
		archive.merge(tmp, "/");
	}
	
	private void addJars() {
		// http://exitcondition.alrubinger.com/2012/09/13/shrinkwrap-resolver-new-api
		final PomEquippedResolveStage pomResolver = Maven.resolver().offline().loadPomFromFile("pom.xml");
		archive.addAsLibraries(pomResolver.resolve("org.richfaces:richfaces")
				                          .withTransitivity()
				                          .asFile());
	}
	
	private void addKlassen() {
		final JavaArchive tmp = ShrinkWrap.create(JavaArchive.class);
		tmp.as(ExplodedImporter.class).importDirectory(CLASSES_DIR);
		archive.merge(tmp, "WEB-INF/classes");
	}
	
	private void addTestKlassen() {
		for (Class<?> c : TEST_CLASSES) {
			archiveMitTestklassen.addClass(c);
		}
	}

	public static ArchiveBuilder getInstance() {
		return INSTANCE;
	}
	
	public Archive<? extends Archive<?>> getArchive() {
		return archive;
	}

	public Archive<? extends Archive<?>> getArchiveMitTestklassen() {
		return archiveMitTestklassen;
	}
}
