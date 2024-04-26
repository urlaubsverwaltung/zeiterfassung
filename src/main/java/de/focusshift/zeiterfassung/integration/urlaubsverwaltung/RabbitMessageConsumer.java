package de.focusshift.zeiterfassung.integration.urlaubsverwaltung;

import org.slf4j.Logger;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    protected RabbitMessageConsumer() {
        //
    }

    /**
     * Maps the source value to the given enum, returns empty Optional when the value cannot be parsed to the enum.
     *
     * @param source source object that should be mapped to an enum
     * @param enumClass class of the enum
     * @return the mapped enum value
     * @param <E> type of the enum
     */
    protected static <E extends Enum<E>> Optional<E> mapToEnum(String source, Class<E> enumClass) {
        try {
            return Optional.of(Enum.valueOf(enumClass, source));
        } catch (IllegalArgumentException e) {
            LOG.info("could not map source={} to enum={}", source, enumClass.getName());
            return Optional.empty();
        }
    }
}
