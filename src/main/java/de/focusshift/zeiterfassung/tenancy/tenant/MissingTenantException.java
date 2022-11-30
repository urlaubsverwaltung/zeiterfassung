package de.focusshift.zeiterfassung.tenancy.tenant;

public class MissingTenantException extends RuntimeException {
    MissingTenantException(String message) {
        super(message);
    }
}
