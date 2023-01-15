package de.focusshift.launchpad.core;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "launchpad")
class LaunchpadConfigProperties {

    private List<App> apps = List.of();

    List<App> getApps() {
        return apps;
    }

    void setApps(List<App> apps) {
        this.apps = apps;
    }

    @Validated
    record App(@Valid @NotNull URL url, @NotEmpty String messageKey, @NotEmpty String icon) {}
}
