package de.focusshift.zeiterfassung.projecttype;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "project_type")
class ProjectTypeEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @SequenceGenerator(name = "project_type_seq", sequenceName = "project_type_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_type_seq")
    private Long id;

    @Column(name = "name", nullable = false)
    @NotNull
    @Size(max = 255)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected ProjectTypeEntity() {
        super(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectTypeEntity that = (ProjectTypeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProjectTypeEntity{" +
            "id=" + id +
            ", name='" + name + '\'' +
            '}';
    }
}
