package de.focusshift.zeiterfassung.importer;

import de.focusshift.zeiterfassung.importer.model.TenantExport;

import java.util.Optional;

interface ImportInputProvider {
    Optional<TenantExport> fromExport();
}
