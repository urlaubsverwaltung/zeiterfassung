package de.focusshift.zeiterfassung.usermanagement;

import java.util.Objects;

class PermissionsDto {

    private boolean viewReportAll;
    private boolean workingTimeEditAll;
    private boolean overtimeEditAll;
    private boolean permissionsEditAll;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionsDto that = (PermissionsDto) o;
        return viewReportAll == that.viewReportAll
            && workingTimeEditAll == that.workingTimeEditAll
            && overtimeEditAll == that.overtimeEditAll
            && permissionsEditAll == that.permissionsEditAll;
    }

    @Override
    public int hashCode() {
        return Objects.hash(viewReportAll, workingTimeEditAll, overtimeEditAll, permissionsEditAll);
    }

    @Override
    public String toString() {
        return "PermissionsDto{" +
            "viewReportAll=" + viewReportAll +
            ", workingTimeEditAll=" + workingTimeEditAll +
            ", overtimeEditAll=" + overtimeEditAll +
            ", permissionsEditAll=" + permissionsEditAll +
            '}';
    }
}
