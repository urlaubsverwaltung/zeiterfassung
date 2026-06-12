package de.focusshift.zeiterfassung.search;

class DefaultUserSearchUiFragmentSupplier implements UserSearchUiFragmentSupplier {

    @Override
    public String get() {
        return "fragments/user-search::user-search-container";
    }
}
