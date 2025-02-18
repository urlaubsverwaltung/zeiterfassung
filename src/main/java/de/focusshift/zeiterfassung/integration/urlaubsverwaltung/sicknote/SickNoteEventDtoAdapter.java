package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.sicknote;

import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteAcceptedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCancelledEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteConvertedToApplicationEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteCreatedEventDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePeriodDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNotePersonDTO;
import de.focus_shift.urlaubsverwaltung.extension.api.sicknote.SickNoteUpdatedEventDTO;

/**
 * Common interface of urlaubsverwaltung sicknote events to provide minimal information required by Zeiterfassung.
 */
class SickNoteEventDtoAdapter {

    private final Long sourceId;
    private final SickNotePersonDTO person;
    private final String type;
    private final SickNotePeriodDTO period;

    SickNoteEventDtoAdapter(SickNoteAcceptedEventDTO event) {
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.type = event.getType();
        this.period = event.getPeriod();
    }

    SickNoteEventDtoAdapter(SickNoteCreatedEventDTO event) {
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.type = event.getType();
        this.period = event.getPeriod();
    }

    SickNoteEventDtoAdapter(SickNoteUpdatedEventDTO event) {
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.type = event.getType();
        this.period = event.getPeriod();
    }

    SickNoteEventDtoAdapter(SickNoteConvertedToApplicationEventDTO event) {
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.type = event.getType();
        this.period = event.getPeriod();
    }

    SickNoteEventDtoAdapter(SickNoteCancelledEventDTO event) {
        this.sourceId = event.getSourceId();
        this.person = event.getPerson();
        this.type = event.getType();
        this.period = event.getPeriod();
    }

    public Long getSourceId() {
        return sourceId;
    }

    public SickNotePersonDTO getPerson() {
        return person;
    }

    public String getType() {
        return type;
    }

    public SickNotePeriodDTO getPeriod() {
        return period;
    }
}
