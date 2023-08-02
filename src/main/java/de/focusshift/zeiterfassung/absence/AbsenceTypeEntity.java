package de.focusshift.zeiterfassung.absence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AbsenceTypeEntity {

    @Column(name = "type_category", nullable = false)
    private String category;

    @Column(name = "type_source_id")
    private Integer sourceId;

    public String getCategory() {
        return category;
    }

    public AbsenceTypeEntity(String name, Integer sourceId) {
        this.category = name;
        this.sourceId = sourceId;
    }

    public AbsenceTypeEntity() {
        // for @Embeddable
    }

    public void setCategory(String name) {
        this.category = name;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }
}
