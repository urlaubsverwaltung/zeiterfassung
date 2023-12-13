package de.focusshift.zeiterfassung.timeentry.settings;

import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Optional;

@Service
class TimeEntrySettingsServiceImpl implements TimeEntrySettingsService {

    private final TimeEntrySettingsRepository repository;

    TimeEntrySettingsServiceImpl(TimeEntrySettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public TimeEntrySettings getTimeEntrySettings() {
        final Iterator<TimeEntrySettingsEntity> iterator = repository.findAll().iterator();
        if (iterator.hasNext()) {
            return toTimeEntrySettings(iterator.next());
        } else {
            return defaultTimeEntrySettings();
        }
    }

    @Override
    public TimeEntrySettings updateTimeEntrySettings(TimeEntryFreeze timeEntryFreeze) {

        final TimeEntrySettingsEntity entity = getEntity().orElseGet(TimeEntrySettingsEntity::new);
        entity.setTimeEntryFreezeEnabled(timeEntryFreeze.enabled());
        entity.setTimeEntryFreezeValue(timeEntryFreeze.value());
        entity.setTimeEntryFreezeUnit(timeEntryFreeze.unit());

        final TimeEntrySettingsEntity saved = repository.save(entity);
        return toTimeEntrySettings(saved);
    }

    private Optional<TimeEntrySettingsEntity> getEntity() {
        final Iterator<TimeEntrySettingsEntity> iterator = repository.findAll().iterator();
        if (iterator.hasNext()) {
            return Optional.of(iterator.next());
        } else {
            return Optional.empty();
        }
    }

    private TimeEntrySettings toTimeEntrySettings(TimeEntrySettingsEntity entity) {

        final TimeEntryFreeze timeEntryFreeze = new TimeEntryFreeze(
            entity.isTimeEntryFreezeEnabled(),
            entity.getTimeEntryFreezeValue(),
            entity.getTimeEntryFreezeUnit()
        );

        return new TimeEntrySettings(timeEntryFreeze);
    }

    private static TimeEntrySettings defaultTimeEntrySettings() {
        final TimeEntryFreeze timeEntryFreeze = new TimeEntryFreeze(false, 0, TimeEntryFreeze.Unit.NONE);
        return new TimeEntrySettings(timeEntryFreeze);
    }
}
