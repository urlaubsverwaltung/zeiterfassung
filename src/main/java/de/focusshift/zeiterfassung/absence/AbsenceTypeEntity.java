package de.focusshift.zeiterfassung.absence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;

import static jakarta.persistence.EnumType.STRING;

@Embeddable
public class AbsenceTypeEntity {

    @Column(name = "type_category", nullable = false)
    @Enumerated(STRING)
    private AbsenceTypeCategory category;

    @Column(name = "type_source_id")
    private Long sourceId;

    public AbsenceTypeEntity(AbsenceTypeCategory category, Long sourceId) {
        this.category = category;
        this.sourceId = sourceId;
    }

    public AbsenceTypeEntity() {
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
}
