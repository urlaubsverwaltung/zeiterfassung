package de.focusshift.launchpad.core;

import java.util.Objects;

public class AppDto {

    private final String url;
    private final String name;
    private final String icon;

    AppDto(String url, String name, String icon) {
        this.url = url;
        this.name = name;
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "AppDto{" +
            "url='" + url + '\'' +
            ", name='" + name + '\'' +
            ", icon='" + icon + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppDto appDto = (AppDto) o;
        return Objects.equals(url, appDto.url) && Objects.equals(name, appDto.name) && Objects.equals(icon, appDto.icon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, name, icon);
    }
}
