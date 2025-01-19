package de.focusshift.zeiterfassung.data.history;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantAwareRevisionEntity;
import de.focusshift.zeiterfassung.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.envers.repository.support.DefaultRevisionMetadata;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityRevisionMapperTest {

    EntityRevisionMapper sut;

    @BeforeEach
    void setUp() {
        sut = new EntityRevisionMapper();
    }

    @Test
    void ensureRevisionIsMappedToMetadata() {

        final long epochMilli = Instant.now().toEpochMilli();

        final TenantAwareRevisionEntity entity = new TenantAwareRevisionEntity();
        entity.setId(42);
        entity.setTimestamp(epochMilli);
        entity.setUpdatedBy("updatedBy");

        final DefaultRevisionMetadata metadata = new DefaultRevisionMetadata(entity, RevisionMetadata.RevisionType.INSERT);
        final Revision<Integer, Object> revision = Revision.of(metadata, entity);

        final EntityRevisionMetadata actual = sut.toEntityRevisionMetadata(revision);
        assertThat(actual.revision()).isEqualTo(42);
        assertThat(actual.modifiedAt()).isEqualTo(Instant.ofEpochMilli(epochMilli));
        assertThat(actual.modifiedBy()).hasValue(new UserId("updatedBy"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    @NullSource
    void ensureRevisionIsMappedToMetadataWithoutUpdatedBy(String givenUpdatedBy) {

        final TenantAwareRevisionEntity entity = new TenantAwareRevisionEntity();
        entity.setUpdatedBy(givenUpdatedBy);

        final DefaultRevisionMetadata metadata = new DefaultRevisionMetadata(entity, RevisionMetadata.RevisionType.INSERT);
        final Revision<Integer, Object> revision = Revision.of(metadata, entity);

        final EntityRevisionMetadata actual = sut.toEntityRevisionMetadata(revision);
        assertThat(actual.modifiedBy()).isEmpty();
    }

    static Stream<Arguments> revisions() {
        return Stream.of(
            Arguments.of(RevisionMetadata.RevisionType.INSERT, EntityRevisionType.CREATED),
            Arguments.of(RevisionMetadata.RevisionType.UPDATE, EntityRevisionType.UPDATED),
            Arguments.of(RevisionMetadata.RevisionType.DELETE, EntityRevisionType.DELETED)
        );
    }

    @ParameterizedTest
    @MethodSource("revisions")
    void ensureCorrectMappedRevisionType(RevisionMetadata.RevisionType revisionType, EntityRevisionType expectedRevisionType) {

        final TenantAwareRevisionEntity entity = new TenantAwareRevisionEntity();

        final Revision<Integer, Object> revision = Revision.of(new DefaultRevisionMetadata(entity, revisionType), entity);
        final EntityRevisionMetadata actual = sut.toEntityRevisionMetadata(revision);

        assertThat(actual.entityRevisionType()).isEqualTo(expectedRevisionType);
    }

    @Test
    void ensureThrowsForRevisionTypeUnknown() {

        final TenantAwareRevisionEntity entity = new TenantAwareRevisionEntity();

        final Revision<Integer, Object> revision = Revision.of(new DefaultRevisionMetadata(entity, RevisionMetadata.RevisionType.UNKNOWN), entity);

        assertThatThrownBy(() -> sut.toEntityRevisionMetadata(revision))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Expected defined revision type, got UNKNOWN.");
    }
}
