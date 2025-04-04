package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerSecurityIT extends SingleTenantTestContainersBase implements ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = {"ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"}, mode = EnumSource.Mode.EXCLUDE)
    void ensureGetSettingsForbidden(SecurityRole role) throws Exception {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockMvc.perform(get("/settings").with(oidcSubject(userIdComposite, List.of(role))))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = {"ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"}, mode = EnumSource.Mode.INCLUDE)
    void ensureGetSettingsIsAllowed(SecurityRole role) throws Exception {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockMvc.perform(get("/settings").with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER, role))))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = {"ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"}, mode = EnumSource.Mode.EXCLUDE)
    void ensureUpdateSettingsForbidden(SecurityRole role) throws Exception {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockMvc.perform(post("/settings").with(oidcSubject(userIdComposite, List.of(role))))
            .andExpect(status().isForbidden());
    }

    @Disabled("This returns 403 instead of 200. Why?")
    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, names = {"ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"}, mode = EnumSource.Mode.INCLUDE)
    void ensureUpdateSettingsIsAllowed(SecurityRole role) throws Exception {

        final UserLocalId userLocalId = new UserLocalId(1L);
        final UserId userId = new UserId(UUID.randomUUID().toString());
        final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);

        mockMvc.perform(post("/settings").with(oidcSubject(userIdComposite, List.of(ZEITERFASSUNG_USER, role)))
                .param("federalState", "NONE")
                .param("worksOnPublicHoliday", "false")
                .param("lockingIsActive", "false")
                .param("lockTimeEntriesDaysInPast", "1")
            )
            .andExpect(status().isOk());
    }
}
