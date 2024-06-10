package de.focusshift.zeiterfassung.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantService;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import de.focusshift.zeiterfassung.timeclock.TimeClockService;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.usermanagement.OvertimeAccountService;
import de.focusshift.zeiterfassung.workingtime.WorkingTimeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "zeiterfassung.tenant.import", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(TenantImportConfigurationProperties.class)
public class TenantImportConfiguration {

    @Bean
    ImportInputProvider importInputProvider(ObjectMapper objectMapper, TenantImportConfigurationProperties tenantImportConfigurationProperties) {
        return new FilesystemBasedImportInputProvider(objectMapper, tenantImportConfigurationProperties.filesystem().path());
    }

    @Bean
    TenantImporterComponent tenantImporterComponent(TenantContextHolder tenantContextHolder,
                                                    TenantService tenantService,
                                                    TenantUserService tenantUserService,
                                                    OvertimeAccountService overtimeAccountService,
                                                    TimeClockService timeClockService,
                                                    TimeEntryService timeEntryService,
                                                    WorkingTimeService workingTimeService,
                                                    ImportInputProvider importInputProvider) {
        return new TenantImporterComponent(tenantContextHolder, tenantService, tenantUserService,
            overtimeAccountService, timeClockService, timeEntryService, workingTimeService, importInputProvider);
    }
}
