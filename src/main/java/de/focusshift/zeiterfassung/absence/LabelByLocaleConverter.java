package de.focusshift.zeiterfassung.absence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Converter
public final class LabelByLocaleConverter implements AttributeConverter<Map<Locale, String>, String> {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<Locale, String> attribute) {
        try {
            return om.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            LOG.error("could not write value as string", ex);
            return null;
        }
    }

    @Override
    public Map<Locale, String> convertToEntityAttribute(String dbData) {
        try {
            return om.readValue(dbData, new TypeReference<>() {
            });
        } catch (IOException ex) {
            LOG.error("could not convert to entity attribute", ex);
            return Map.of();
        }
    }
}
