package de.focusshift.zeiterfassung.development;

import de.focusshift.zeiterfassung.absence.AbsenceTypeService;
import de.focusshift.zeiterfassung.absence.AbsenceWriteService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.development.demodata.create", havingValue = "true")
@EnableConfigurationProperties(DemoDataProperties.class)
public class DemoDataConfiguration {

    @Bean
    DemoDataCreationService demoDataCreationService(TimeEntryService timeEntryService,
                                                    AbsenceWriteService absenceWriteService,
                                                    AbsenceTypeService absenceTypeService,
                                                    DemoDataProperties demoDataProperties) {
        return new DemoDataCreationService(timeEntryService, absenceWriteService, absenceTypeService, demoDataProperties);
    }
}
