package de.focusshift.zeiterfassung;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

/**
 * Represents an immutable date range.
 * <p>
 * A date range represents a period of time between two LocalDates.
 * Date range are inclusive of the start and the end date.
 * The end date is always greater than or equal to the start date.
 * <p>
 */
public record DateRange(LocalDate startDate, LocalDate endDate) implements Iterable<LocalDate> {

    public DateRange {
        notNull(startDate, "expected startDate not to be null");
        notNull(endDate, "expected endDate not to be null");
        isTrue(!startDate.isAfter(endDate), "expected startDate not to be after endDate");
    }

    @Override
    public Iterator<LocalDate> iterator() {
        return new DateRangeIterator(startDate, endDate);
    }

    private static final class DateRangeIterator implements Iterator<LocalDate> {

        private final LocalDate endDate;
        private LocalDate cursor;

        DateRangeIterator(LocalDate startDate, LocalDate endDate) {
            this.cursor = startDate;
            this.endDate = endDate;
        }

        @Override
        public boolean hasNext() {
            return cursor.isBefore(endDate) || cursor.isEqual(endDate);
        }

        @Override
        public LocalDate next() {

            if (cursor.isAfter(endDate)) {
                throw new NoSuchElementException("next date is after endDate which is not in range anymore.");
            }

            final LocalDate current = cursor;
            cursor = cursor.plusDays(1);
            return current;
        }
    }
}
