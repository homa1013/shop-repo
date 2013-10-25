package de.shop.kundenverwaltung.domain;

import static de.shop.util.Constants.ERSTE_VERSION;
import static de.shop.util.Constants.KEINE_ID;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.FetchType.EAGER;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.TemporalType.DATE;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonSubTypes.Type;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.ScriptAssert;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.jaxb.Formatted;

import de.shop.auth.domain.RolleType;
import de.shop.bestellverwaltung.domain.Bestellung;
import de.shop.util.persistence.File;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Entity
//Zu email wird unten ein UNIQUE Index definiert
@Table(name = "kunde", indexes = { @Index(columnList = "nachname"), @Index(columnList = "file_fk")})
@Inheritance  // Alternativen: strategy = SINGLE_TABLE (=default), TABLE_PER_CLASS, JOINED
@DiscriminatorColumn(name = "art", length = 1)
@NamedQueries({
	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN,
                query = "SELECT  k"
				        + " FROM AbstractKunde k"),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN_ORDER_BY_ID,
		        query = "SELECT   k"
				        + " FROM  AbstractKunde k"
		                + " ORDER BY k.id"),
	@NamedQuery(name  = AbstractKunde.FIND_IDS_BY_PREFIX,
	            query = "SELECT   k.id"
				        + " FROM  AbstractKunde k"
				        + " WHERE CONCAT('', k.id) LIKE :" + AbstractKunde.PARAM_KUNDE_ID_PREFIX
				        + " ORDER BY k.id"),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN_BY_ID_PREFIX,
		        query = "SELECT   k"
		                + " FROM  AbstractKunde k"
		                + " WHERE CONCAT('', k.id) LIKE :" + AbstractKunde.PARAM_KUNDE_ID_PREFIX
		                + " ORDER BY k.id"),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN_BY_NACHNAME,
	            query = "SELECT   k"
				        + " FROM  AbstractKunde k"
	            		+ " WHERE UPPER(k.nachname) = UPPER(:" + AbstractKunde.PARAM_KUNDE_NACHNAME + ")"),
	// FIXME https://hibernate.atlassian.net/browse/HHH-8285 : @NamedEntityGraph ab Java EE 7 bzw. JPA 2.1
    @NamedQuery(name  = AbstractKunde.FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN,
   	            query = "SELECT      DISTINCT k"
   			            + " FROM     AbstractKunde k LEFT JOIN FETCH k.bestellungen"
   			            + " WHERE    k.id = :" + AbstractKunde.PARAM_KUNDE_ID),
    @NamedQuery(name  = AbstractKunde.FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN,
	            query = "SELECT      DISTINCT k"
			            + " FROM     AbstractKunde k LEFT JOIN FETCH k.bestellungen"
			            + " WHERE    UPPER(k.nachname) = UPPER(:" + AbstractKunde.PARAM_KUNDE_NACHNAME + ")"
			            + " ORDER BY k.id"),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDE_BY_EMAIL,
                query = "SELECT   k"
			            + " FROM  AbstractKunde k"
            		    + " WHERE UPPER(k.email) = UPPER(:" + AbstractKunde.PARAM_KUNDE_EMAIL + ")"),
	@NamedQuery(name  = AbstractKunde.FIND_NACHNAMEN_BY_PREFIX,
   	            query = "SELECT   DISTINCT k.nachname"
				        + " FROM  AbstractKunde k "
   	            		+ " WHERE UPPER(k.nachname) LIKE UPPER(:"
   	            		+ AbstractKunde.PARAM_KUNDE_NACHNAME_PREFIX + ")"),
    @NamedQuery(name  = AbstractKunde.FIND_ALL_NACHNAMEN,
   	            query = "SELECT      DISTINCT k.nachname"
   				        + " FROM     AbstractKunde k"
   				        + " ORDER BY k.nachname"),
   	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN_OHNE_BESTELLUNGEN,
	            query = "SELECT k"
			            + " FROM   AbstractKunde k"
			            + " WHERE  k.bestellungen IS EMPTY"),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDEN_BY_PLZ,
	            query = "SELECT   k"
				        + " FROM  AbstractKunde k"
			            + " WHERE k.adresse.plz = :" + AbstractKunde.PARAM_KUNDE_ADRESSE_PLZ),
	@NamedQuery(name  = AbstractKunde.FIND_KUNDE_BY_USERNAME,
	            query = "SELECT   k"
				        + " FROM  AbstractKunde k"
	            		+ " WHERE CONCAT('', k.id) = :" + AbstractKunde.PARAM_KUNDE_USERNAME),
	@NamedQuery(name  = AbstractKunde.FIND_USERNAME_BY_USERNAME_PREFIX,
  	            query = "SELECT   CONCAT('', k.id)"
  				        + " FROM  AbstractKunde k"
   	            		+ " WHERE CONCAT('', k.id) LIKE :" + AbstractKunde.PARAM_USERNAME_PREFIX),

	@NamedQuery(name = AbstractKunde.FIND_KUNDEN_BY_DATE,
   			    query = "SELECT   k"
   			            + " FROM  AbstractKunde k"
   			    		+ " WHERE k.seit = :" + AbstractKunde.PARAM_KUNDE_SEIT),
   	@NamedQuery(name = AbstractKunde.FIND_PRIVATKUNDEN_FIRMENKUNDEN,
   			    query = "SELECT   k"
   			            + " FROM  AbstractKunde k"
   			    		+ " WHERE TYPE(k) IN (Privatkunde, Firmenkunde)")

})
@NamedEntityGraphs({
	@NamedEntityGraph(name = AbstractKunde.GRAPH_BESTELLUNGEN,
					  attributeNodes = @NamedAttributeNode("bestellungen")),
	@NamedEntityGraph(name = AbstractKunde.GRAPH_WARTUNGSVERTRAEGE,
					  attributeNodes = @NamedAttributeNode("wartungsvertraege"))
})
@ScriptAssert(lang = "javascript",
	          script = "_this.password != null && !_this.password.equals(\"\")"
	          		   + "&& _this.password.equals(_this.passwordWdh)",
	          message = "{kunde.password.notEqual}",
	          groups = { Default.class, PasswordGroup.class })
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
	@Type(value = Privatkunde.class, name = AbstractKunde.PRIVATKUNDE),
	@Type(value = Firmenkunde.class, name = AbstractKunde.FIRMENKUNDE) })
@XmlRootElement
@Formatted
@XmlSeeAlso({ Firmenkunde.class, Privatkunde.class })
public abstract class AbstractKunde implements Serializable, Cloneable {
	private static final long serialVersionUID = 4190012483483113483L;
	private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
	
	private static final String NAME_PATTERN = "[A-Z\u00C4\u00D6\u00DC][a-z\u00E4\u00F6\u00FC\u00DF]+";
	private static final String PREFIX_ADEL = "(o'|von|von der|von und zu|van)?";
	public static final String NACHNAME_PATTERN = PREFIX_ADEL + NAME_PATTERN + "(-" + NAME_PATTERN + ")?";
	private static final int NACHNAME_LENGTH_MIN = 2;   // kann an der GUI abgefragt werden
	private static final int NACHNAME_LENGTH_MAX = 32;
	private static final int VORNAME_LENGTH_MAX = 32;
	private static final int PASSWORD_LENGTH_MAX = 256;
	private static final int EMAIL_LENGTH_MAX = 32;
	private static final String RABATT_MAX = "0.5";
	private static final int BEMERKUNGEN_LENGTH_MAX = 2000;
	
	public static final String PRIVATKUNDE = "P";
	public static final String FIRMENKUNDE = "F";
	
	private static final String PREFIX = "AbstractKunde.";
	public static final String FIND_KUNDEN = PREFIX + "findKunden";
	public static final String FIND_KUNDEN_ORDER_BY_ID = PREFIX + "findKundenOrderById";
	public static final String FIND_IDS_BY_PREFIX = PREFIX + "findIdsByIdPrefix";
	public static final String FIND_KUNDEN_BY_ID_PREFIX = PREFIX + "findKundenByIdPrefix";
	public static final String FIND_KUNDEN_BY_NACHNAME = PREFIX + "findKundenByNachname";
	// FIXME https://hibernate.atlassian.net/browse/HHH-8285 : @NamedEntityGraph ab Java EE 7 bzw. JPA 2.1
	public static final String FIND_KUNDE_BY_ID_FETCH_BESTELLUNGEN =
                               PREFIX + "findKundeByIdFetchBestellungen";
	public static final String FIND_KUNDEN_BY_NACHNAME_FETCH_BESTELLUNGEN =
		                       PREFIX + "findKundenByNachnameFetchBestellungen";
	public static final String FIND_KUNDE_BY_EMAIL = PREFIX + "findKundeByEmail";
	public static final String FIND_NACHNAMEN_BY_PREFIX = PREFIX + "findNachnamenByPrefix";
	public static final String FIND_ALL_NACHNAMEN = PREFIX + "findAllNachnamen";
	public static final String FIND_KUNDEN_OHNE_BESTELLUNGEN = PREFIX + "findKundenOhneBestellungen";
	public static final String FIND_KUNDEN_BY_PLZ = PREFIX + "findKundenByPlz";
	public static final String FIND_KUNDE_BY_USERNAME = PREFIX + "findKundeByUsername";
	public static final String FIND_USERNAME_BY_USERNAME_PREFIX = PREFIX + "findKundeByUsernamePrefix";
	public static final String FIND_KUNDEN_BY_DATE = PREFIX + "findKundenByDate";
	public static final String FIND_PRIVATKUNDEN_FIRMENKUNDEN = PREFIX + "findPrivatkundenFirmenkunden";

	public static final String PARAM_KUNDE_ID = "id";
	public static final String PARAM_KUNDE_ID_PREFIX = "idPrefix";
	public static final String PARAM_KUNDE_NACHNAME = "nachname";
	public static final String PARAM_KUNDE_NACHNAME_PREFIX = "nachnamePrefix";
	public static final String PARAM_KUNDE_ADRESSE_PLZ = "plz";
	public static final String PARAM_KUNDE_USERNAME = "username";
	public static final String PARAM_USERNAME_PREFIX = "usernamePrefix";
	public static final String PARAM_KUNDE_EMAIL = "email";
	public static final String PARAM_KUNDE_SEIT = "seit";
	
	public static final String GRAPH_BESTELLUNGEN = "bestellungen";
	public static final String GRAPH_WARTUNGSVERTRAEGE = "wartungsvertraege";

	
	@Id
	@GeneratedValue
	@Column(nullable = false, updatable = false)
	private Long id = KEINE_ID;

	@Version
	@Basic(optional = false)
	private int version = ERSTE_VERSION;

	@Column(length = NACHNAME_LENGTH_MAX, nullable = false)
	@NotNull(message = "{kunde.nachname.notNull}")
	@Size(min = NACHNAME_LENGTH_MIN,
		  max = NACHNAME_LENGTH_MAX,
		  message = "{kunde.nachname.length}")
	@Pattern(regexp = NACHNAME_PATTERN, message = "{kunde.nachname.pattern}")
	private String nachname = "";

	@Column(length = VORNAME_LENGTH_MAX)
	@Size(max = VORNAME_LENGTH_MAX, message = "{kunde.vorname.length}")
	private String vorname = "";
	
	@Basic(optional = false)
	private short kategorie;
	
	@Column(precision = 5, scale = 4)
	@DecimalMax(value = RABATT_MAX, message = "{kunde.rabatt.max}")
	private BigDecimal rabatt = BigDecimal.ZERO;
	
	@Column(precision = 15, scale = 3)
	private BigDecimal umsatz = BigDecimal.ZERO;
	
	@Basic(optional = false)
	@Temporal(DATE)
	@Past(message = "{kunde.seit.past}")
	private Date seit;
	
	@Column(length = EMAIL_LENGTH_MAX, unique = true, nullable = false)
	@Email(message = "{kunde.email}")
	private String email = "";
	
	private boolean newsletter = false;

	@Column(length = PASSWORD_LENGTH_MAX)
	@Size(max = PASSWORD_LENGTH_MAX, message = "{kunde.password.length}")
	private String password;
	
	@Transient
	private String passwordWdh;

//  siehe @ScriptAssert
//	@AssertTrue(/*groups = PasswordGroup.class,*/ message = "{kunde.password.notEqual}")
//	public boolean isPasswortEqual() {
//		if (password == null) {
//			return passwordWdh == null;
//		}
//		return password.equals(passwordWdh);
//	}
	
	@Transient
	@AssertTrue(message = "{kunde.agb}")
	private boolean agbAkzeptiert;

	@OneToOne(mappedBy = "kunde", cascade = { PERSIST, REMOVE })
	@Valid
	@NotNull(message = "{kunde.adresse.notNull}")
	private Adresse adresse;
	
	// Default: fetch = LAZY, keine Kaskadierungen
	// Alternativen: cascade = PERSIST, cascade = { PERSIST, REMOVE } usw.
	@OneToMany
	@JoinColumn(name = "kunde_fk", nullable = false)
	@OrderColumn(name = "idx", nullable = false)
	@XmlTransient
	private List<Bestellung> bestellungen;
	
	@Transient
	private URI bestellungenUri;
	
	@OneToMany
	@JoinColumn(name = "kunde_fk", nullable = false)
	@OrderColumn(name = "idx", nullable = false)
	@XmlTransient
	private List<Wartungsvertrag> wartungsvertraege;
	
	@ElementCollection(fetch = EAGER)
	@CollectionTable(name = "kunde_rolle",
	                 joinColumns = @JoinColumn(name = "kunde_fk", nullable = false),
   	                 uniqueConstraints =  @UniqueConstraint(columnNames = { "kunde_fk", "rolle" }))
	@Column(table = "kunde_rolle", name = "rolle", length = 32, nullable = false)
	private Set<RolleType> rollen;
	
	@OneToOne(fetch = LAZY, cascade = { PERSIST, REMOVE })
	@JoinColumn(name = "file_fk")
	@XmlTransient
	private File file;
	
	@Column
	@Size(max = BEMERKUNGEN_LENGTH_MAX)
	//@SafeHtml
	private String bemerkungen;
	
	@Basic(optional = false)
	@Temporal(TIMESTAMP)
	@XmlTransient
	private Date erzeugt;

	@Basic(optional = false)
	@Temporal(TIMESTAMP)
	@XmlTransient
	private Date aktualisiert;
	
	public AbstractKunde() {
		super();
	}

	public AbstractKunde(String nachname, String vorname, String email, Date seit) {
		super();
		this.nachname = nachname;
		this.vorname = vorname;
		this.email = email;
		this.seit = seit == null ? null : (Date) seit.clone();
	}
	
	@PrePersist
	protected void prePersist() {
		erzeugt = new Date();
		aktualisiert = new Date();
	}
	
	@PostPersist
	protected void postPersist() {
		LOGGER.debugf("Neuer Kunde mit ID=%d", id);
	}

	@PreUpdate
	protected void preUpdate() {
		aktualisiert = new Date();
	}
	
	@PostUpdate
	protected void postUpdate() {
		LOGGER.debugf("Kunde mit ID=%d aktualisiert: version=%d", id, version);
	}
	
	@PostLoad
	protected void postLoad() {
		passwordWdh = password;
		agbAkzeptiert = true;
	}
	
	public void setValues(AbstractKunde k) {
		version = k.version;
		nachname = k.nachname;
		vorname = k.vorname;
		kategorie = k.kategorie;
		umsatz = k.umsatz;
		seit = k.seit;
		email = k.email;
		password = k.password;
		passwordWdh = k.password;
		agbAkzeptiert = k.agbAkzeptiert;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}

	public String getNachname() {
		return nachname;
	}
	public void setNachname(String nachname) {
		this.nachname = nachname;
	}

	public String getVorname() {
		return vorname;
	}
	public void setVorname(String vorname) {
		this.vorname = vorname;
	}
	
	public short getKategorie() {
		return kategorie;
	}

	public void setKategorie(short kategorie) {
		this.kategorie = kategorie;
	}
	
	public BigDecimal getRabatt() {
		return rabatt;
	}
	public void setRabatt(BigDecimal rabatt) {
		this.rabatt = rabatt;
	}

	public BigDecimal getUmsatz() {
		return umsatz;
	}

	public void setUmsatz(BigDecimal umsatz) {
		this.umsatz = umsatz;
	}

	public Date getSeit() {
		return seit == null ? null : (Date) seit.clone();
	}

	public void setSeit(Date seit) {
		this.seit = seit == null ? null : (Date) seit.clone();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public void setNewsletter(boolean newsletter) {
		this.newsletter = newsletter;
	}

	public boolean isNewsletter() {
		return newsletter;
	}

	
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPasswordWdh() {
		return passwordWdh;
	}

	public void setPasswordWdh(String passwordWdh) {
		this.passwordWdh = passwordWdh;
	}
	
	public void setAgbAkzeptiert(boolean agbAkzeptiert) {
		this.agbAkzeptiert = agbAkzeptiert;
	}

	public boolean isAgbAkzeptiert() {
		return agbAkzeptiert;
	}

	public Adresse getAdresse() {
		return adresse;
	}
	public void setAdresse(Adresse adresse) {
		this.adresse = adresse;
	}

	public List<Bestellung> getBestellungen() {
		if (bestellungen == null) {
			return null;
		}
		return Collections.unmodifiableList(bestellungen);
	}
	public void setBestellungen(List<Bestellung> bestellungen) {
		if (this.bestellungen == null) {
			this.bestellungen = bestellungen;
			return;
		}
		
		// Wiederverwendung der vorhandenen Collection
		this.bestellungen.clear();
		if (bestellungen != null) {
			this.bestellungen.addAll(bestellungen);
		}
	}
	
	public AbstractKunde addBestellung(Bestellung bestellung) {
		if (bestellungen == null) {
			bestellungen = new ArrayList<>();
		}
		bestellungen.add(bestellung);
		return this;
	}

	public URI getBestellungenUri() {
		return bestellungenUri;
	}

	public void setBestellungenUri(URI bestellungenUri) {
		this.bestellungenUri = bestellungenUri;
	}

	public List<Wartungsvertrag> getWartungsvertraege() {
		if (wartungsvertraege == null) {
			return null;
		}
		
		return Collections.unmodifiableList(wartungsvertraege);
	}

	public void setWartungsvertraege(List<Wartungsvertrag> wartungsvertraege) {
		if (this.wartungsvertraege == null) {
			this.wartungsvertraege = wartungsvertraege;
			return;
		}
		
		// Wiederverwendung der vorhandenen Collection
		this.wartungsvertraege.clear();
		if (wartungsvertraege != null) {
			this.wartungsvertraege.addAll(wartungsvertraege);
		}
	}
	
	public AbstractKunde addWartungsvertrag(Wartungsvertrag wartungsvertrag) {
		if (wartungsvertraege == null) {
			wartungsvertraege = new ArrayList<>();
		}
		wartungsvertraege.add(wartungsvertrag);
		return this;
	}

	public Set<RolleType> getRollen() {
		if (rollen == null) {
			return null;
		}
		
		return Collections.unmodifiableSet(rollen);
	}

	public void setRollen(Set<RolleType> rollen) {
		if (this.rollen == null) {
			this.rollen = rollen;
			return;
		}
		
		// Wiederverwendung der vorhandenen Collection
		this.rollen.clear();
		if (rollen != null) {
			this.rollen.addAll(rollen);
		}
	}
	
	public AbstractKunde addRollen(Collection<RolleType> rollen) {
		LOGGER.tracef("neue Rollen: %s", rollen);
		if (this.rollen == null) {
			this.rollen = new HashSet<>();
		}
		this.rollen.addAll(rollen);
		LOGGER.tracef("Rollen nachher: %s", this.rollen);
		return this;
	}
	
	public AbstractKunde removeRollen(Collection<RolleType> rollen) {
		LOGGER.tracef("zu entfernende Rollen: %s", rollen);
		if (this.rollen == null) {
			return this;
		}
		this.rollen.removeAll(rollen);
		LOGGER.tracef("Rollen nachher: %s", this.rollen);
		return this;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getBemerkungen() {
		return bemerkungen;
	}

	public void setBemerkungen(String bemerkungen) {
		this.bemerkungen = bemerkungen;
	}

	public Date getErzeugt() {
		return erzeugt == null ? null : (Date) erzeugt.clone();
	}

	public void setErzeugt(Date erzeugt) {
		this.erzeugt = erzeugt == null ? null : (Date) erzeugt.clone();
	}

	public Date getAktualisiert() {
		return aktualisiert == null ? null : (Date) aktualisiert.clone();
	}

	public void setAktualisiert(Date aktualisiert) {
		this.aktualisiert = aktualisiert == null ? null : (Date) aktualisiert.clone();
	}

	@Override
	public String toString() {
		return "Kunde [id=" + id + ", version=" + version
			   + ", nachname=" + nachname + ", vorname=" + vorname + ", seit=" + seit
			   + ", email=" + email + ", kategorie=" + kategorie + ", rabatt=" + rabatt
			   + ", bestellungenUri=" + bestellungenUri
			   + ", rollen=" + rollen + ", password=" + password + ", passwordWdh=" + passwordWdh
			   + ", erzeugt=" + erzeugt + ", aktualisiert=" + aktualisiert + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractKunde other = (AbstractKunde) obj;
		if (email == null) {
			if (other.email != null) {
				return false;
			}
		}
		else if (!email.equals(other.email)) {
			return false;
		}
		return true;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		final AbstractKunde neuesObjekt = AbstractKunde.class.cast(super.clone());
		neuesObjekt.id = id;
		neuesObjekt.version = version;
		neuesObjekt.nachname = nachname;
		neuesObjekt.vorname = vorname;
		neuesObjekt.kategorie = kategorie;
		neuesObjekt.umsatz = umsatz;
		neuesObjekt.email = email;
		neuesObjekt.newsletter = newsletter;
		neuesObjekt.password = password;
		neuesObjekt.passwordWdh = passwordWdh;
		neuesObjekt.agbAkzeptiert = agbAkzeptiert;
		neuesObjekt.adresse = adresse;
		neuesObjekt.bemerkungen = bemerkungen;
		neuesObjekt.erzeugt = erzeugt;
		neuesObjekt.aktualisiert = aktualisiert;
		return neuesObjekt;
	}
}
