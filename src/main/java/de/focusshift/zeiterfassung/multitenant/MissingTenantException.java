package de.focusshift.zeiterfassung.multitenant;

public class MissingTenantException extends RuntimeException {
    MissingTenantException(String message) {
        super(message);
    }
}
