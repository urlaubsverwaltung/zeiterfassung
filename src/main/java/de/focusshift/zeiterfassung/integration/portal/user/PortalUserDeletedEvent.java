package de.focusshift.zeiterfassung.integration.portal.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;


@JsonIgnoreProperties(ignoreUnknown = true)
record PortalUserDeletedEvent(
    String uuid,
    String tenantId,
    String firstName,
    String lastName,
    String email,
    boolean emailVerified,
    Instant createdAt,
    Instant updatedAt,
    Instant deletedAt,
    String status
) {

}
