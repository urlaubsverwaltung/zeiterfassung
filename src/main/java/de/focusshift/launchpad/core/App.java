package de.focusshift.launchpad.core;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public final class App {

    private final URL url;
    private final AppName appName;
    private final String icon;
    private final String authority;

    App(URL url, AppName appName, String icon) {
        this(url, appName, icon, null);
    }

    App(URL url, AppName appName, String icon, String authority) {
        this.url = url;
        this.appName = appName;
        this.icon = icon;
        this.authority = authority;
    }

    public URL getUrl() {
        return url;
    }

    public AppName getAppName() {
        return appName;
    }

    public String getIcon() {
        return icon;
    }

    public Optional<String> getAuthority() {
        return Optional.ofNullable(authority);
    }

    @Override
    public String toString() {
        return "App{" +
            "url=" + url +
            ", appName=" + appName +
            ", icon='" + icon + '\'' +
            ", authority='" + authority + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        App app = (App) o;
        return Objects.equals(url, app.url) && Objects.equals(appName, app.appName) && Objects.equals(icon, app.icon) && Objects.equals(authority, app.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, appName, icon, authority);
    }
}
