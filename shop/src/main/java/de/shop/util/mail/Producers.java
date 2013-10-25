package de.shop.util.mail;

import java.io.Serializable;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@ApplicationScoped
public class Producers implements Serializable {
	private static final long serialVersionUID = 3916523726340426731L;
	
	// In src\webapp\WEB-INF\web.xml koennen die einzelnen Werte gesetzt bzw. ueberschrieben werden

	@Resource(name = "absenderMail")
	@Produces
	@AbsenderMail
	private String absenderMail;
	
	@Resource(name = "absenderName")
	@Produces
	@AbsenderName
	private String absenderName;
	
	@Resource(name = "empfaengerMail")
	@Produces
	@EmpfaengerMail
	private String empfaengerMail;
	
	@Resource(name = "empfaengerName")
	@Produces
	@EmpfaengerName
	private String empfaengerName;
}
