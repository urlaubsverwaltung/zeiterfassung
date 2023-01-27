package de.focusshift.launchpad.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "launchpad")
class LaunchpadConfigProperties {

    @NotNull
    private Locale nameDefaultLocale;

    private List<App> apps = List.of();

    public Locale getNameDefaultLocale() {
        return nameDefaultLocale;
    }

    public void setNameDefaultLocale(Locale nameDefaultLocale) {
        this.nameDefaultLocale = nameDefaultLocale;
    }

    List<App> getApps() {
        return apps;
    }

    void setApps(List<App> apps) {
        this.apps = apps;
    }

    @Validated
    record App(@NotNull String url, @NotNull Map<Locale, String> name, @NotEmpty String icon, String authority) {}
}
