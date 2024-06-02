package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity(name = "absence_type")
public class AbsenceTypeEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "absence_type_seq", sequenceName = "absence_type_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "absence_type_seq")
    private Long id;

    @Column(name = "category", nullable = false)
    @Enumerated(STRING)
    private AbsenceTypeCategory category;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "color", nullable = false)
    @Enumerated(STRING)
    private AbsenceColor color;

    @Column(name = "label_by_locale", nullable = false)
    @Convert(converter = LabelByLocaleConverter.class)
    private Map<Locale, String> labelByLocale;

    public AbsenceTypeEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AbsenceTypeCategory getCategory() {
        return category;
    }

    public void setCategory(AbsenceTypeCategory category) {
        this.category = category;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public AbsenceColor getColor() {
        return color;
    }

    public void setColor(AbsenceColor color) {
        this.color = color;
    }

    public Map<Locale, String> getLabelByLocale() {
        return labelByLocale;
    }

    public void setLabelByLocale(Map<Locale, String> labelByLocale) {
        this.labelByLocale = labelByLocale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsenceTypeEntity that = (AbsenceTypeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AbsenceTypeEntity{" +
            ", id=" + id +
            ", category=" + category +
            ", sourceId=" + sourceId +
            ", color=" + color +
            ", labelByLocale=" + labelByLocale +
            '}';
    }
}
