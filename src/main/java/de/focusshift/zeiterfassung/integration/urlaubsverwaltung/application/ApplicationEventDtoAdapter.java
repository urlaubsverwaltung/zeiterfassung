package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.application;

import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationAllowedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationCreatedFromSickNoteEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.ApplicationPersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.application.VacationTypeDTO;

import java.time.LocalDate;
import java.util.Set;

/**
 * Common interface of urlaubsverwaltung (vacation)application events to provide minimal information required by Zeiterfassung.
 */
class ApplicationEventDtoAdapter {

    private final String tenantId;
    private final Integer sourceId;
    private final ApplicationPersonDTO person;
    private final VacationTypeDTO vacationType;
    private final ApplicationPeriodDTO period;
    private final Set<LocalDate> absentWorkingDays;

    ApplicationEventDtoAdapter(ApplicationAllowedEventDTO event) {
        this.tenantId = event.getTenantId();
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.vacationType = event.getVacationType();
        this.period = event.getPeriod();
        this.absentWorkingDays = event.getAbsentWorkingDays();
    }

    ApplicationEventDtoAdapter(ApplicationCancelledEventDTO event) {
        this.tenantId = event.getTenantId();
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.vacationType = event.getVacationType();
        this.period = event.getPeriod();
        this.absentWorkingDays = event.getAbsentWorkingDays();
    }

    ApplicationEventDtoAdapter(ApplicationCreatedFromSickNoteEventDTO event) {
        this.tenantId = event.getTenantId();
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.vacationType = event.getVacationType();
        this.period = event.getPeriod();
        this.absentWorkingDays = event.getAbsentWorkingDays();
    }

    public String getTenantId() {
        return tenantId;
    }

    public Integer getSourceId() {
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

    public Set<LocalDate> getAbsentWorkingDays() {
        return absentWorkingDays;
    }
}
