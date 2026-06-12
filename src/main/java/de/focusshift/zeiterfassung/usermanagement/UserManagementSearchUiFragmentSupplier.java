package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.search.UserSearchUiFragmentSupplier;
import org.springframework.stereotype.Component;

@Component
class UserManagementSearchUiFragmentSupplier implements UserSearchUiFragmentSupplier {

    @Override
    public String get() {
        return "usermanagement/fragments/user-search::search";
    }
}
