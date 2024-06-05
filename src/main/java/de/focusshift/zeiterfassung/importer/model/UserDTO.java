package de.focusshift.zeiterfassung.importer.model;

import java.time.Instant;
import java.util.Set;

public record UserDTO(String externalId, String givenName, String familyName, String eMail, Instant firstLoginAt,
                      Set<String> authorities) {
}
