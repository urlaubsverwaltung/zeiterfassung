package de.focusshift.launchpad.core;

import java.util.List;
import java.util.Objects;

public class LaunchpadDto {

    private final List<AppDto> apps;

    LaunchpadDto(List<AppDto> apps) {
        this.apps = apps;
    }

    public List<AppDto> getApps() {
        return apps;
    }

    @Override
    public String toString() {
        return "LaunchpadDto{" +
            "apps=" + apps +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LaunchpadDto that = (LaunchpadDto) o;
        return Objects.equals(apps, that.apps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apps);
    }
}
