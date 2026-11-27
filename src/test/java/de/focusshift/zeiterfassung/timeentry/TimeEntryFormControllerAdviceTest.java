package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.settings.TimeEntrySettings;
import de.focusshift.zeiterfassung.settings.TimeEntrySettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeEntryFormControllerAdviceTest {

    @Test
    void ensureTimeEntrySettingsAreAddedToModel() {
        final TimeEntrySettingsService service = mock(TimeEntrySettingsService.class);
        final TimeEntrySettings settings = new TimeEntrySettings(false, true, false);
        when(service.getTimeEntrySettings()).thenReturn(settings);

        final Model model = new ConcurrentModel();
        new TimeEntryFormControllerAdvice(service).addAttributes(model);

        assertThat(model.getAttribute("timeEntrySettings")).isEqualTo(settings);
    }
}
