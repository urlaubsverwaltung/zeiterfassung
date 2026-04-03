package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.web.html.AriaCurrent;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

class NavigationItemDto {

    private final String id;
    private final String href;
    private final String messageKey;
    private final String dataTestId;
    private final boolean active;
    private final AriaCurrent ariaCurrent;

    NavigationItemDto(String id, String href, String messageKey) {
        this(id, href, messageKey, false, null);
    }

    NavigationItemDto(String id, String href, String messageKey, boolean active,  AriaCurrent ariaCurrent) {
        this(id, href, messageKey, active, ariaCurrent, null);
    }

    NavigationItemDto(String id, String href, String messageKey, boolean active, AriaCurrent ariaCurrent, String dataTestId) {
        this.id = id;
        this.href = href;
        this.messageKey = messageKey;
        this.active = active;
        this.ariaCurrent = requireNonNullElse(ariaCurrent, AriaCurrent.FALSE);
        this.dataTestId = dataTestId;
    }

    public String getId() {
        return id;
    }

    public String getHref() {
        return href;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean isActive() {
        return active;
    }

    public String ariaCurrent() {
        return ariaCurrent.getValue();
    }

    public String getDataTestId() {
        return dataTestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NavigationItemDto that = (NavigationItemDto) o;
        return id.equals(that.id)
            && href.equals(that.href)
            && messageKey.equals(that.messageKey)
            && Objects.equals(active, that.active)
            && Objects.equals(ariaCurrent, that.ariaCurrent)
            && Objects.equals(dataTestId, that.dataTestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, href, messageKey, active, ariaCurrent, dataTestId);
    }

    @Override
    public String toString() {
        return "NavigationItemDto{" +
            "id='" + id + '\'' +
            ", href='" + href + '\'' +
            ", messageKey='" + messageKey + '\'' +
            ", active='" + active + '\'' +
            ", ariaCurrent='" + ariaCurrent + '\'' +
            ", dataTestId='" + dataTestId + '\'' +
            '}';
    }
}
