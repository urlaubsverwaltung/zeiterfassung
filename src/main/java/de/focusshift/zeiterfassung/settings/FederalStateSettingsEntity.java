package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity(name = "settings_federal_state")
public class FederalStateSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_federal_state_seq", sequenceName = "settings_federal_state_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_federal_state_seq")
    protected Long id;

    @Column(name = "federal_state")
    @Enumerated(STRING)
    private FederalState federalState;

    @Column(name = "works_on_public_holiday")
    private boolean worksOnPublicHoliday;

    protected FederalStateSettingsEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FederalState getFederalState() {
        return federalState;
    }

    public void setFederalState(FederalState federalState) {
        this.federalState = federalState;
    }

    public boolean isWorksOnPublicHoliday() {
        return worksOnPublicHoliday;
    }

    public void setWorksOnPublicHoliday(boolean worksOnPublicHoliday) {
        this.worksOnPublicHoliday = worksOnPublicHoliday;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FederalStateSettingsEntity that = (FederalStateSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FederalStateSettingsEntity{" +
            "id=" + id +
            ", federalState=" + federalState +
            ", worksOnPublicHoliday=" + worksOnPublicHoliday +
            '}';
    }
}
