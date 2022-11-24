package de.focusshift.zeiterfassung.usermanagement;

class UserAccountAuthoritiesDto {

    private boolean user;
    private boolean viewReportAll;

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

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private boolean user;
        private boolean viewReportAll;

        public Builder user(boolean user) {
            this.user = user;
            return this;
        }

        public Builder viewReportAll(boolean viewReportAll) {
            this.viewReportAll = viewReportAll;
            return this;
        }

        public UserAccountAuthoritiesDto build() {
            final UserAccountAuthoritiesDto authoritiesDto = new UserAccountAuthoritiesDto();
            authoritiesDto.setUser(user);
            authoritiesDto.setViewReportAll(viewReportAll);
            return authoritiesDto;
        }
    }
}
