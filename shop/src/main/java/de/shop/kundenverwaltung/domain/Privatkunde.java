package de.shop.kundenverwaltung.domain;

import static de.shop.kundenverwaltung.domain.AbstractKunde.PRIVATKUNDE;
import static javax.persistence.FetchType.EAGER;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Entity
@Inheritance
@DiscriminatorValue(PRIVATKUNDE)
@NamedQueries({
	@NamedQuery(name  = Privatkunde.FIND_BY_GESCHLECHT,
                query = "SELECT  k"
				        + " FROM Privatkunde k"
                		+ " WHERE k.geschlecht = :" + Privatkunde.PARAM_GESCHLECHT)
})
@Cacheable
@XmlRootElement
public class Privatkunde extends AbstractKunde {
	private static final long serialVersionUID = -1783340753647408724L;
	
	private static final String PREFIX = "Privatkunde.";
	public static final String FIND_BY_GESCHLECHT = PREFIX + "findByGeschlecht";
	public static final String PARAM_GESCHLECHT = "geschlecht";
	
	@Column(length = 2)
	private FamilienstandType familienstand = FamilienstandType.VERHEIRATET;
	
	@Column(length = 1)
	private GeschlechtType geschlecht = GeschlechtType.WEIBLICH;
	
	@ElementCollection(fetch = EAGER)
	@CollectionTable(name = "kunde_hobby",
	                 joinColumns = @JoinColumn(name = "kunde_fk", nullable = false),
                     uniqueConstraints =  @UniqueConstraint(columnNames = { "kunde_fk", "hobby" }))
	@Column(table = "kunde_hobby", name = "hobby", length = 2, nullable = false)
	private Set<HobbyType> hobbies;
	
	public Privatkunde() {
		super();
	}
	
	public Privatkunde(String nachname, String vorname, String email, Date seit) {
		super(nachname, vorname, email, seit);
	}
	
	@Override
	public void setValues(AbstractKunde k) {
		super.setValues(k);
		
		if (!k.getClass().equals(Privatkunde.class)) {
			return;
		}
		
		final Privatkunde pk = (Privatkunde) k;
		familienstand = pk.familienstand;
		geschlecht = pk.geschlecht;
		hobbies = pk.hobbies;
	}
	
	public FamilienstandType getFamilienstand() {
		return familienstand;
	}
	
	public void setFamilienstand(FamilienstandType familienstand) {
		this.familienstand = familienstand;
	}

	public GeschlechtType getGeschlecht() {
		return geschlecht;
	}
	public void setGeschlecht(GeschlechtType geschlecht) {
		this.geschlecht = geschlecht;
	}
	public Set<HobbyType> getHobbies() {
		if (hobbies == null) {
			return null;
		}
		return Collections.unmodifiableSet(hobbies);
	}
	
	public void setHobbies(Set<HobbyType> hobbies) {
		if (this.hobbies == null) {
			this.hobbies = hobbies;
			return;
		}
		
		// Wiederverwendung der vorhandenen Collection
		this.hobbies.clear();
		if (hobbies != null) {
			this.hobbies.addAll(hobbies);
		}
	}

	@Override
	public String toString() {
		return "Privatkunde [" + super.toString() + ", familienstand=" + familienstand
		       + ", geschlecht=" + geschlecht + ", hobbies=" + hobbies + "]";
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// Fuer Validierung an der Benutzeroberflaeche
		final Privatkunde neuesObjekt = Privatkunde.class.cast(super.clone());
		
		neuesObjekt.familienstand = familienstand;
		neuesObjekt.geschlecht = geschlecht;
		neuesObjekt.hobbies = hobbies;
		
		return neuesObjekt;
	}
}
