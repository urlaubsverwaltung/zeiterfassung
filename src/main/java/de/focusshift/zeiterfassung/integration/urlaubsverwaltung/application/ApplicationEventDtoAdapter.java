package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationUpdatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;

import java.time.Duration;
import java.util.Optional;

/**
 * Common interface of urlaubsverwaltung (vacation)application events to provide minimal information required by Zeiterfassung.
 */
class ApplicationEventDtoAdapter {

    private final Long sourceId;
    private final ApplicationPersonDTO person;
    private final VacationTypeDTO vacationType;
    private final ApplicationPeriodDTO period;
    private final Duration overtimeHours;

    ApplicationEventDtoAdapter(ApplicationAllowedEventDTO event) {
        this.sourceId = event.sourceId();
        this.person = event.person();
        this.vacationType = event.vacationType();
        this.period = event.period();
        this.overtimeHours = event.hours();
    }

    ApplicationEventDtoAdapter(ApplicationCancelledEventDTO event) {
        this.sourceId = event.sourceId();
        this.person = event.person();
        this.vacationType = event.vacationType();
        this.period = event.period();
        this.overtimeHours = event.hours();
    }

    ApplicationEventDtoAdapter(ApplicationUpdatedEventDTO event) {
        this.sourceId = event.sourceId();
        this.person = event.person();
        this.vacationType = event.vacationType();
        this.period = event.period();
        this.overtimeHours = event.hours();
    }

    ApplicationEventDtoAdapter(ApplicationCreatedFromSickNoteEventDTO event) {
        this.sourceId = event.sourceId();
        this.person = event.person();
        this.vacationType = event.vacationType();
        this.period = event.period();
        this.overtimeHours = null;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public ApplicationPersonDTO getPerson() {
        return person;
    }

    public VacationTypeDTO getVacationType() {
        return vacationType;
    }

    public ApplicationPeriodDTO getPeriod() {
        return period;
    }

    public Optional<Duration> getOvertimeHours() {
        return Optional.ofNullable(overtimeHours);
    }
}
