package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.tenancy.tenant.AbstractTenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "user_settings")
public class UserSettingsEntity extends AbstractTenantAwareEntity {

    @Id
    @Column(name = "tenant_user_local_id")
    private Long tenantUserLocalId;

    @Column(name = "theme", nullable = false)
    @NotNull
    @Enumerated(STRING)
    private Theme theme;

    @Column(name = "locale")
    private Locale locale;

    @Column(name = "locale_browser_specific")
    private Locale localeBrowserSpecific;

    @Column(name = "navigation_collapsed", nullable = false)
    private boolean navigationCollapsed;

    protected UserSettingsEntity() {
        super(null);
    }

    protected UserSettingsEntity(String tenantId, Long tenantUserLocalId, Theme theme, @Nullable Locale locale, @Nullable Locale localeBrowserSpecific) {
        super(tenantId);
        this.tenantUserLocalId = tenantUserLocalId;
        this.theme = theme;
        this.locale = locale;
        this.localeBrowserSpecific = localeBrowserSpecific;
    }

    public Long getTenantUserLocalId() {
        return tenantUserLocalId;
    }

    public void setTenantUserLocalId(Long tenantUserLocalId) {
        this.tenantUserLocalId = tenantUserLocalId;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public @Nullable Locale getLocale() {
        return locale;
    }

    public void setLocale(@Nullable Locale locale) {
        this.locale = locale;
    }

    public @Nullable Locale getLocaleBrowserSpecific() {
        return localeBrowserSpecific;
    }

    public void setLocaleBrowserSpecific(@Nullable Locale localeBrowserSpecific) {
        this.localeBrowserSpecific = localeBrowserSpecific;
    }

    public boolean isNavigationCollapsed() {
        return navigationCollapsed;
    }

    public void setNavigationCollapsed(boolean navigationCollapsed) {
        this.navigationCollapsed = navigationCollapsed;
    }

    @Override
    public String toString() {
        return "UserSettingsEntity{" +
            "tenantUserLocalId=" + tenantUserLocalId +
            ", theme=" + theme +
            ", navigationCollapsed=" + navigationCollapsed +
            ", locale=" + locale +
            ", localeBrowserSpecific=" + localeBrowserSpecific +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserSettingsEntity that = (UserSettingsEntity) o;
        return Objects.equals(tenantUserLocalId, that.tenantUserLocalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantUserLocalId);
    }
}
