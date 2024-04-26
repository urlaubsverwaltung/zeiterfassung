package de.focusshift.zeiterfassung.integration.urlaubsverwaltung;

import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

public abstract class RabbitMessageConsumer {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    protected RabbitMessageConsumer() {
        //
    }

    /**
     *
     * @param source source object that should be mapped to an enum
     * @param mapper mapper function
     * @param errorMessageSupplier message logged when mapping fails
     * @return the mapped enum value
     * @param <T> type of the source value
     * @param <R> type of the enum
     */
    protected static <T, R extends Enum<R>> Optional<R> mapToEnum(T source, Function<T, R> mapper, Supplier<String> errorMessageSupplier) {
        try {
            return Optional.of(mapper.apply(source));
        } catch (IllegalArgumentException e) {
            LOG.info(errorMessageSupplier.get());
            return Optional.empty();
        }
    }
}
