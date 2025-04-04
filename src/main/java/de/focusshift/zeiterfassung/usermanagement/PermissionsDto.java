package de.focusshift.zeiterfassung.usermanagement;

class PermissionsDto {

    private boolean viewReportAll;
    private boolean workingTimeEditAll;
    private boolean overtimeEditAll;
    private boolean permissionsEditAll;
    private boolean timeEntryEditAll;
    private boolean globalSettings;

    public boolean isViewReportAll() {
        return viewReportAll;
    }

    public void setViewReportAll(boolean viewReportAll) {
        this.viewReportAll = viewReportAll;
    }

    public boolean isWorkingTimeEditAll() {
        return workingTimeEditAll;
    }

    public void setWorkingTimeEditAll(boolean workingTimeEditAll) {
        this.workingTimeEditAll = workingTimeEditAll;
    }

    public boolean isOvertimeEditAll() {
        return overtimeEditAll;
    }

    public void setOvertimeEditAll(boolean overtimeEditAll) {
        this.overtimeEditAll = overtimeEditAll;
    }

    public boolean isPermissionsEditAll() {
        return permissionsEditAll;
    }

    public void setPermissionsEditAll(boolean permissionsEditAl) {
        this.permissionsEditAll = permissionsEditAl;
    }

    public void setTimeEntryEditAll(boolean timeEntryEditAll) {
        this.timeEntryEditAll = timeEntryEditAll;
    }

    public boolean isTimeEntryEditAll() {
        return timeEntryEditAll;
    }

    public boolean isGlobalSettings() {
        return globalSettings;
    }

    public void setGlobalSettings(boolean globalSettings) {
        this.globalSettings = globalSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        PermissionsDto that = (PermissionsDto) o;
        return viewReportAll == that.viewReportAll
            && workingTimeEditAll == that.workingTimeEditAll
            && overtimeEditAll == that.overtimeEditAll
            && permissionsEditAll == that.permissionsEditAll
            && timeEntryEditAll == that.timeEntryEditAll
            && globalSettings == that.globalSettings;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(viewReportAll);
        result = 31 * result + Boolean.hashCode(workingTimeEditAll);
        result = 31 * result + Boolean.hashCode(overtimeEditAll);
        result = 31 * result + Boolean.hashCode(permissionsEditAll);
        result = 31 * result + Boolean.hashCode(timeEntryEditAll);
        result = 31 * result + Boolean.hashCode(globalSettings);
        return result;
    }

    @Override
    public String toString() {
        return "PermissionsDto{" +
            "viewReportAll=" + viewReportAll +
            ", workingTimeEditAll=" + workingTimeEditAll +
            ", overtimeEditAll=" + overtimeEditAll +
            ", permissionsEditAll=" + permissionsEditAll +
            ", timeEntryEditAll=" + timeEntryEditAll +
            ", globalSettings=" + globalSettings +
            '}';
    }
}
