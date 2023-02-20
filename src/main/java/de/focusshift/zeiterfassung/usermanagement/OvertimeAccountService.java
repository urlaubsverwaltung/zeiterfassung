package de.focusshift.zeiterfassung.usermanagement;

public interface OvertimeAccountService {

    /**
     * Get the {@linkplain OvertimeAccount} of the user or a default overtime account.
     *
     * @param userLocalId user local id
     * @return the {@linkplain OvertimeAccount}, never {@code null}.
     */
    OvertimeAccount getOvertimeAccount(UserLocalId userLocalId);

    /**
     * Update the {@linkplain OvertimeAccount}
     *
     * @param overtimeAccount {@linkplain OvertimeAccount} to update
     * @return the updated {@linkplain OvertimeAccount}
     */
    OvertimeAccount updateOvertimeAccount(OvertimeAccount overtimeAccount);
}
