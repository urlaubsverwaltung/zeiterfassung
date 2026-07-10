package de.focusshift.zeiterfassung.timeentry.republish;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * RabbitMQ payload that triggers republishing of
 * {@link de.focusshift.zeiterfassung.timeentry.events.DayLockedEvent} for the given inclusive date range.
 *
 * @param tenantId optional tenant to republish for; when {@code null} or blank the events are republished for all active tenants
 * @param from     first day to republish (inclusive)
 * @param to       last day to republish (inclusive)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record DayLockedRepublishRequest(
    @Nullable String tenantId,
    LocalDate from,
    LocalDate to
) {
}
