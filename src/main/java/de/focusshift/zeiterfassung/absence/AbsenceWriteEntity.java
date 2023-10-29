package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.configuration.multi.AdminAware;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "absence")
public class AbsenceWriteEntity implements AdminAware<Long> {

    @Size(max = 255)
    @Column(name = "tenant_id")
    private String tenantId;

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "absence_seq", sequenceName = "absence_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "absence_seq")
    private Long id;

    @Column(name="source_id", nullable = false)
    private Long sourceId;

    @Column(name="user_id", nullable = false)
    private String userId;

    @Column(name="start_date", nullable = false)
    private Instant startDate;

    @Column(name="end_date", nullable = false)
    private Instant endDate;

    @Column(name="day_length", nullable = false)
    @Enumerated(STRING)
    private DayLength dayLength;

    @Embedded
    private AbsenceTypeEntityEmbeddable type;

    @Column(nullable = false)
    @Enumerated(STRING)
    private AbsenceColor color;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public DayLength getDayLength() {
        return dayLength;
    }

    public void setDayLength(DayLength dayLength) {
        this.dayLength = dayLength;
    }

    public AbsenceTypeEntityEmbeddable getType() {
        return type;
    }

    public void setType(AbsenceTypeEntityEmbeddable type) {
        this.type = type;
    }

    public AbsenceColor getColor() {
        return color;
    }

    public void setColor(AbsenceColor color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbsenceWriteEntity that = (AbsenceWriteEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AbsenceEntity{" +
            "tenantId=" + tenantId +
            ", id=" + id +
            '}';
    }
}
