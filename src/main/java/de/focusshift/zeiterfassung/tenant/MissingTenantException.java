package de.focusshift.zeiterfassung.tenant;

public class MissingTenantException extends RuntimeException {
    MissingTenantException(String message) {
        super(message);
    }
}
