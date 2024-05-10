package de.focusshift.zeiterfassung.absence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;

import static jakarta.persistence.EnumType.STRING;

@Embeddable
public class AbsenceTypeEntityEmbeddable {

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
}
