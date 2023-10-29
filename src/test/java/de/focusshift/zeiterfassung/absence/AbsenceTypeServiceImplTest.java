package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceTypeServiceImplTest {

    @InjectMocks
    private AbsenceTypeServiceImpl sut;

    @Mock
    private AbsenceTypeRepository repository;

    @Test
    void ensureCreatingNewAbsenceType() {

        when(repository.findByTenantIdAndSourceId("tenant", 42L)).thenReturn(Optional.empty());

        final AbsenceTypeUpdate absenceTypeUpdate = new AbsenceTypeUpdate(
            new TenantId("tenant"),
            42L,
            AbsenceTypeCategory.OTHER,
            AbsenceColor.VIOLET,
            Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            )
        );

        sut.updateAbsenceType(absenceTypeUpdate);

        final ArgumentCaptor<AbsenceTypeEntity> captor = ArgumentCaptor.forClass(AbsenceTypeEntity.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getId()).isNull();
            assertThat(entity.getTenantId()).isEqualTo("tenant");
            assertThat(entity.getCategory()).isEqualTo(AbsenceTypeCategory.OTHER);
            assertThat(entity.getColor()).isEqualTo(AbsenceColor.VIOLET);
            assertThat(entity.getLabelByLocale()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            ));
        });
    }

    @Test
    void ensureUpdateAbsenceType() {

        final AbsenceTypeEntity existingEntity = new AbsenceTypeEntity();
        existingEntity.setId(1L);

        when(repository.findByTenantIdAndSourceId("tenant", 42L))
            .thenReturn(Optional.of(existingEntity));

        final AbsenceTypeUpdate absenceTypeUpdate = new AbsenceTypeUpdate(
            new TenantId("tenant"),
            42L,
            AbsenceTypeCategory.OTHER,
            AbsenceColor.VIOLET,
            Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            )
        );

        sut.updateAbsenceType(absenceTypeUpdate);

        final ArgumentCaptor<AbsenceTypeEntity> captor = ArgumentCaptor.forClass(AbsenceTypeEntity.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue()).satisfies(entity -> {
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getTenantId()).isEqualTo("tenant");
            assertThat(entity.getCategory()).isEqualTo(AbsenceTypeCategory.OTHER);
            assertThat(entity.getColor()).isEqualTo(AbsenceColor.VIOLET);
            assertThat(entity.getLabelByLocale()).containsExactlyInAnyOrderEntriesOf(Map.of(
                Locale.GERMAN, "label-de",
                Locale.ENGLISH, "label-en"
            ));
        });
    }
}
