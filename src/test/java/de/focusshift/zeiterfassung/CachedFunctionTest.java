package de.focusshift.zeiterfassung;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CachedFunctionTest {

    @Test
    void ensureReturnsCachedValue() {

        AtomicInteger counter = new AtomicInteger();

        final Function<Long, Long> dizzyCalculon = value -> {
            counter.incrementAndGet();
            return 42L;
        };

        final CachedFunction<Long, Long> sut = new CachedFunction<>(dizzyCalculon);

        assertThat(sut.apply(1L)).isEqualTo(42L);
        assertThat(sut.apply(1L)).isEqualTo(42L);
        assertThat(counter.get()).isEqualTo(1);
    }
}
