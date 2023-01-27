package de.focusshift.launchpad.core;

import java.util.List;
import java.util.Objects;

public final class Launchpad {

    private final List<App> apps;

    Launchpad(List<App> apps) {
        this.apps = apps;
    }

    public List<App> getApps() {
        return apps;
    }

    @Override
    public String toString() {
        return "Launchpad{" +
            "apps=" + apps +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Launchpad launchpad = (Launchpad) o;
        return Objects.equals(apps, launchpad.apps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apps);
    }
}
