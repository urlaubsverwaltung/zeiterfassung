package de.focusshift.zeiterfassung.integration.urlaubsverwaltung.companyvacation;

import de.focusshift.zeiterfassung.companyvacation.CompanyVacationWriteService;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.companyvacation.enabled", havingValue = "true")
@EnableConfigurationProperties(CompanyVacationRabbitmqConfigurationProperties.class)
class CompanyVacationRabbitmqConfiguration {

    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_PUBLISHED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.companyvacation.published";
    static final String ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_DELETED_QUEUE = "zeiterfassung.queue.urlaubsverwaltung.companyvacation.deleted";

    @Bean
    CompanyVacationEventHandlerRabbitmq companyVacationEventHandlerRabbitmq(CompanyVacationWriteService companyVacationWriteService, TenantContextHolder tenantContextHolder) {
        return new CompanyVacationEventHandlerRabbitmq(companyVacationWriteService, tenantContextHolder);
    }

    @Configuration
    @ConditionalOnProperty(value = "zeiterfassung.integration.urlaubsverwaltung.companyvacation.manage-topology", havingValue = "true")
    static class ManageTopologyConfiguration {

        private final CompanyVacationRabbitmqConfigurationProperties companyVacationRabbitmqConfigurationProperties;

        ManageTopologyConfiguration(CompanyVacationRabbitmqConfigurationProperties companyVacationRabbitmqConfigurationProperties) {
            this.companyVacationRabbitmqConfigurationProperties = companyVacationRabbitmqConfigurationProperties;
        }

        @Bean
        public TopicExchange companyVacationTopic() {
            return new TopicExchange(companyVacationRabbitmqConfigurationProperties.getTopic());
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungCompanyVacationPublishedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_PUBLISHED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungCompanyVacationPublishedQueue() {
            final String routingKeyPublished = companyVacationRabbitmqConfigurationProperties.getRoutingKeyPublished();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungCompanyVacationPublishedQueue())
                .to(companyVacationTopic())
                .with(routingKeyPublished);
        }

        @Bean
        Queue zeiterfassungUrlaubsverwaltungCompanyVacationDeletedQueue() {
            return new Queue(ZEITERFASSUNG_URLAUBSVERWALTUNG_COMPANY_VACATION_DELETED_QUEUE, true);
        }

        @Bean
        Binding bindZeiterfassungUrlaubsverwaltungApplicationUpdatedQueue() {
            final String routingKeyDeleted = companyVacationRabbitmqConfigurationProperties.getRoutingKeyDeleted();
            return BindingBuilder.bind(zeiterfassungUrlaubsverwaltungCompanyVacationDeletedQueue())
                .to(companyVacationTopic())
                .with(routingKeyDeleted);
        }
    }
}
