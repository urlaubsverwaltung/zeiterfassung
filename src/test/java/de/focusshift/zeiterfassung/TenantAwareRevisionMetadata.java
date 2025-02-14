package de.focusshift.zeiterfassung;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity;
import org.springframework.data.envers.repository.support.DefaultRevisionMetadata;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

/**
 * Copied from {@link DefaultRevisionMetadata} and adjusted to identifier type {@link Long}.
 */
public class TenantAwareRevisionMetadata implements RevisionMetadata<Long> {

    private final TenantAwareRevisionEntity entity;
    private final RevisionType revisionType;

    public TenantAwareRevisionMetadata(TenantAwareRevisionEntity entity) {
        this(entity, RevisionType.UNKNOWN);
    }

    public TenantAwareRevisionMetadata(TenantAwareRevisionEntity entity, RevisionType revisionType) {

        Assert.notNull(entity, "TenantAwareRevisionEntity must not be null");

        this.entity = entity;
        this.revisionType = revisionType;
    }

    public Optional<Long> getRevisionNumber() {
        return Optional.of(entity.getId());
    }

    @Deprecated
    public Optional<LocalDateTime> getRevisionDate() {
        return getRevisionInstant().map(instant -> LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
    }

    @Override
    public Optional<Instant> getRevisionInstant() {
        return Optional.of(Instant.ofEpochMilli(entity.getTimestamp()));
    }

    @SuppressWarnings("unchecked")
    public <T> T getDelegate() {
        return (T) entity;
    }

    @Override
    public RevisionType getRevisionType() {
        return revisionType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TenantAwareRevisionMetadata that = (TenantAwareRevisionMetadata) o;
        return Objects.equals(getRevisionNumber(), that.getRevisionNumber())
            && Objects.equals(getRevisionInstant(), that.getRevisionInstant())
            && revisionType == that.revisionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRevisionNumber(), getRevisionInstant(), revisionType);
    }

    @Override
    public String toString() {
        return "TenantAwareRevisionMetadata{" + "entity=" + entity + ", revisionType=" + revisionType + '}';
    }
}
