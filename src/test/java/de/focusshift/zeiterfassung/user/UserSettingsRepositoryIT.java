package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.SingleTenantTestContainersBase;
import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.tenancy.user.TenantUser;
import de.focusshift.zeiterfassung.tenancy.user.TenantUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static de.focusshift.zeiterfassung.user.Theme.SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserSettingsRepositoryIT extends SingleTenantTestContainersBase {

    @Autowired
    private UserSettingsRepository sut;

    @Autowired
    private TenantUserService tenantUserService;

    @Test
    void ensuresToFindUserSettingsByUserLocalId() {

        final String uuid1 = "1a432ba3-cb93-463b-813b-8e065c1e0a24";
        final EMailAddress eMailAddress1 = new EMailAddress("batman@example.org");
        final TenantUser user1 = tenantUserService.createNewUser(uuid1, "Bruce", "Wayne", eMailAddress1, Set.of());

        final UserSettingsEntity userSetting1 = new UserSettingsEntity();
        userSetting1.setTenantUserLocalId(user1.localId());
        userSetting1.setTheme(SYSTEM);
        userSetting1.setLocale(Locale.GERMAN);
        sut.save(userSetting1);

        final String uuid2 = "8b913da0-2711-4da8-9216-9904e11944ac";
        final EMailAddress eMailAddress2 = new EMailAddress("Clark@example.org");
        final TenantUser user2 = tenantUserService.createNewUser(uuid2, "Kent", "Clark", eMailAddress2, Set.of());

        final UserSettingsEntity userSettings2 = new UserSettingsEntity();
        userSettings2.setTenantUserLocalId(user2.localId());
        userSettings2.setTheme(SYSTEM);
        userSettings2.setLocale(Locale.GERMAN);
        sut.save(userSettings2);

        final Optional<UserSettingsEntity> actual = sut.findByTenantUserLocalId(user2.localId());
        assertThat(actual).hasValue(userSettings2);
    }
}
