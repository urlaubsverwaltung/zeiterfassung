package de.focusshift.zeiterfassung.data.history;

import de.focusshift.zeiterfassung.user.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * Base information for entity revisions.
 *
 * @param revision revision identifier
 * @param entityRevisionType type of the revision
 * @param modifiedAt modification date
 * @param modifiedBy person who modified the entity
 */
public record EntityRevisionMetadata(
    long revision,
    EntityRevisionType entityRevisionType,
    Instant modifiedAt,
    Optional<UserId> modifiedBy
) {
}
