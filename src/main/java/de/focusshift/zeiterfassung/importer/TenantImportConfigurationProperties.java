package de.focusshift.zeiterfassung.importer;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeiterfassung.tenant.import")
public record TenantImportConfigurationProperties(boolean enabled, FilesystemBased filesystem) {

    public record FilesystemBased(String path) {
    }
}
