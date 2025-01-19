package de.focusshift.zeiterfassung.data.history;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity;
import de.focusshift.zeiterfassung.user.UserId;
import org.slf4j.Logger;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EntityRevisionMapper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    public EntityRevisionMetadata toEntityRevisionMetadata(Revision<?, ?> revision) {

        final RevisionMetadata<?> metadata = revision.getMetadata();
        final Optional<TenantAwareRevisionEntity> customMetadata = toTenantAwareRevisionEntity(metadata);

        return new EntityRevisionMetadata(
            metadata.getRequiredRevisionNumber().longValue(),
            toEntityRevisionType(metadata.getRevisionType()),
            metadata.getRequiredRevisionInstant(),
            customMetadata.map(TenantAwareRevisionEntity::getUpdatedBy).filter(StringUtils::hasText).map(UserId::new)
        );
    }

    private static EntityRevisionType toEntityRevisionType(RevisionMetadata.RevisionType revisionType) {
        return switch (revisionType) {
            case UNKNOWN -> throw new IllegalArgumentException("Expected defined revision type, got UNKNOWN.");
            case INSERT -> EntityRevisionType.CREATED;
            case UPDATE -> EntityRevisionType.UPDATED;
            case DELETE -> EntityRevisionType.DELETED;
        };
    }

    private static Optional<TenantAwareRevisionEntity> toTenantAwareRevisionEntity(RevisionMetadata<?> metadata) {

        final Object delegate = metadata.getDelegate();

        if (delegate instanceof TenantAwareRevisionEntity entity) {
            return Optional.of(entity);
        }

        LOG.error("Revision metadata is not our custom implemented RevisionEntity, got {}.", delegate.getClass());
        return Optional.empty();
    }
}
