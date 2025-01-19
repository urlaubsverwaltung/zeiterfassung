package de.focusshift.zeiterfassung.data.history;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Instant;
import java.util.Optional;

public record EntityRevisionMetadata(
    long revision,
    EntityRevisionType entityRevisionType,
    Instant modifiedAt,
    Optional<UserId> modifiedBy
) {
}
