package de.focusshift.zeiterfassung.usermanagement;

class UserAccountAuthoritiesDto {

    private boolean user;
    private boolean viewReportAll;
    private boolean editAuthorities;

    public boolean isUser() {
        return user;
    }

    public void setUser(boolean user) {
        this.user = user;
    }

    public boolean isViewReportAll() {
        return viewReportAll;
    }

    public void setViewReportAll(boolean viewReportAll) {
        this.viewReportAll = viewReportAll;
    }

    public boolean isEditAuthorities() {
        return editAuthorities;
    }

    public void setEditAuthorities(boolean editAuthorities) {
        this.editAuthorities = editAuthorities;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private boolean user;
        private boolean viewReportAll;
        private boolean editAuthorities;

        public Builder user(boolean user) {
            this.user = user;
            return this;
        }

        public Builder viewReportAll(boolean viewReportAll) {
            this.viewReportAll = viewReportAll;
            return this;
        }

        public Builder editAuthorities(boolean editAuthorities) {
            this.editAuthorities = editAuthorities;
            return this;
        }

        public UserAccountAuthoritiesDto build() {
            final UserAccountAuthoritiesDto authoritiesDto = new UserAccountAuthoritiesDto();
            authoritiesDto.setUser(user);
            authoritiesDto.setViewReportAll(viewReportAll);
            authoritiesDto.setEditAuthorities(editAuthorities);
            return authoritiesDto;
        }
    }
}
