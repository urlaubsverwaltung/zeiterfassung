package de.focusshift.zeiterfassung;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import org.slf4j.Logger;

import java.io.IOException;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Persistence attribute converter to map an object into a json string and the other way around.
 */
public final class JpaJsonConverter implements AttributeConverter<Object, String> {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        try {
            return om.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            LOG.error("could not write value as string", ex);
            return null;
        }
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        try {
            return om.readValue(dbData, Object.class);
        } catch (IOException ex) {
            LOG.error("could not convert to entity attribute", ex);
            return null;
        }
    }
}
