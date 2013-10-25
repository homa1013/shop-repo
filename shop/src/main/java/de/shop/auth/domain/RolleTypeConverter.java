package de.shop.auth.domain;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;


/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Converter(autoApply = true)
public class RolleTypeConverter implements AttributeConverter<RolleType, String> {
	@Override
	public String convertToDatabaseColumn(RolleType rolleType) {
		if (rolleType == null) {
			return null;
		}
		return rolleType.getInternal();
	}

	@Override
	public RolleType convertToEntityAttribute(String internal) {
		return RolleType.build(internal);
	}
}
