package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.Map;

public interface OvertimeAccountService {

    /**
     * Get the {@linkplain OvertimeAccount} of the user or a default overtime account.
     *
     * @param userLocalId user local id
     * @return the {@linkplain OvertimeAccount}, never {@code null}.
     */
    OvertimeAccount getOvertimeAccount(UserLocalId userLocalId);

    /**
     * Get all {@link OvertimeAccount}s.
     *
     * @return all {@link OvertimeAccount}s
     */
    Map<UserIdComposite, OvertimeAccount> getAllOvertimeAccounts();

    /**
     * Update the {@linkplain OvertimeAccount}
     *
     * @param userLocalId account of this user should be updated
     * @param isOvertimeAllowed whether overtime is allowed for the user or not
     * @param maxAllowedOvertime optionally maximum allowed overtime duration. may be {@code null}.
     * @return the updated {@linkplain OvertimeAccount}
     */
    OvertimeAccount updateOvertimeAccount(UserLocalId userLocalId, boolean isOvertimeAllowed, @Nullable Duration maxAllowedOvertime);
}
