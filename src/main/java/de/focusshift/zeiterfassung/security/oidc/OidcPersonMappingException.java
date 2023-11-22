package de.focusshift.zeiterfassung.security.oidc;

import org.springframework.security.core.AuthenticationException;

public class OidcPersonMappingException extends AuthenticationException {
    OidcPersonMappingException(String message) {
        super(message);
    }
}
