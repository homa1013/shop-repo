package de.shop.util.persistence;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author <a href="mailto:Juergen.Zimmermann@HS-Karlsruhe.de">J&uuml;rgen Zimmermann</a>
 */
@Converter(autoApply = true)
public class MultimediaTypeConverter implements AttributeConverter<MultimediaType, String> {
	@Override
	public String convertToDatabaseColumn(MultimediaType multimediaType) {
		return multimediaType.getDbString();
	}

	@Override
	public MultimediaType convertToEntityAttribute(String dbString) {
		return MultimediaType.build(dbString);
	}
}
