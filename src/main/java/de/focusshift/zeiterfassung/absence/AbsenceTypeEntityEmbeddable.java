package de.focusshift.zeiterfassung.absence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Embeddable
public class AbsenceTypeEntityEmbeddable implements Serializable {

    @Serial
    private static final long serialVersionUID = -393457998708034020L;

    @Column(name = "type_category", nullable = false)
    @Enumerated(STRING)
    private AbsenceTypeCategory category;

    @Column(name = "type_source_id")
    private Long sourceId;

    public AbsenceTypeEntityEmbeddable(AbsenceTypeCategory category, Long sourceId) {
        this.category = category;
        this.sourceId = sourceId;
    }

    public AbsenceTypeEntityEmbeddable() {
        // for @Embeddable
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsenceTypeEntityEmbeddable that = (AbsenceTypeEntityEmbeddable) o;
        return category == that.category && Objects.equals(sourceId, that.sourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, sourceId);
    }

    @Override
    public String toString() {
        return "AbsenceTypeEntityEmbeddable{" +
            "category=" + category +
            ", sourceId=" + sourceId +
            '}';
    }
}
