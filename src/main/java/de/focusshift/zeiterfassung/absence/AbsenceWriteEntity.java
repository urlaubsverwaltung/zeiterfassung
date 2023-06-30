package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "absence")
class AbsenceWriteEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "absence_seq", sequenceName = "absence_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "absence_seq")
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;

    @Column(nullable = false)
    @Enumerated(STRING)
    private DayLength dayLength;

    @Column(nullable = false)
    @Enumerated(STRING)
    private AbsenceType type;

    @Column(nullable = false)
    @Enumerated(STRING)
    private AbsenceColor color;

    protected AbsenceWriteEntity() {
        this(null, null, null, null, null, null, null, null);
    }

    AbsenceWriteEntity(String tenantId, Long id, String userId, Instant startDate, Instant endDate,
                       DayLength dayLength, AbsenceType type, AbsenceColor color) {
        super(tenantId);
        this.id = id;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dayLength = dayLength;
        this.type = type;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AbsenceType getType() {
        return type;
    }

    public void setType(AbsenceType type) {
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
            "id=" + id +
            '}';
    }
}
