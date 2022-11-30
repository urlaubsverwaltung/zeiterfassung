package de.focusshift.zeiterfassung.tenancy.configuration.multi;

import org.springframework.data.domain.Persistable;

/**
 * simple entity interface used in combination with admin datasource
 */
public interface AdminAware<ID> extends Persistable<ID> {

    default boolean isNew() {
        return getId() == null;
    }
}
