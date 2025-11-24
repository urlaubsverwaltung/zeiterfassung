package de.focusshift.zeiterfassung.absence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Converter
public final class LabelByLocaleConverter implements AttributeConverter<Map<Locale, String>, String> {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final JsonMapper jm = new JsonMapper();

    @Override
    public String convertToDatabaseColumn(Map<Locale, String> attribute) {
        try {
            return jm.writeValueAsString(attribute);
        } catch (JacksonException ex) {
            LOG.error("could not write value as string", ex);
            return null;
        }
    }

    @Override
    public Map<Locale, String> convertToEntityAttribute(String dbData) {
        try {
            return jm.readValue(dbData, new TypeReference<>() {
            });
        } catch (JacksonException ex) {
            LOG.error("could not convert to entity attribute", ex);
            return Map.of();
        }
    }
}
