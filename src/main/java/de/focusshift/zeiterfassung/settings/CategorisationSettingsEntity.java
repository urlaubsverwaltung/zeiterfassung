package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.util.Objects;

@Entity(name = "settings_categorisation")
class CategorisationSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "settings_categorisation_seq", sequenceName = "settings_categorisation_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "settings_categorisation_seq")
    protected Long id;

    @Column(name = "project_required", nullable = false)
    private boolean projectRequired = false;

    @Column(name = "activity_type_required", nullable = false)
    private boolean activityTypeRequired = false;

    protected CategorisationSettingsEntity() {
        super(null);
    }

    public Long getId() { return id; }

    public boolean isProjectRequired() { return projectRequired; }
    public void setProjectRequired(boolean projectRequired) { this.projectRequired = projectRequired; }

    public boolean isActivityTypeRequired() { return activityTypeRequired; }
    public void setActivityTypeRequired(boolean activityTypeRequired) { this.activityTypeRequired = activityTypeRequired; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorisationSettingsEntity that = (CategorisationSettingsEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
