package de.focusshift.zeiterfassung.user;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
class UserSettingsApiController {

    private final UserSettingsService userSettingsService;

    UserSettingsApiController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @PatchMapping
    ResponseEntity<Void> updateSettings(@CurrentUser CurrentOidcUser currentUser, @RequestBody UpdateUserSettingsDto dto) {
        if (dto.navigationCollapsed() != null) {
            userSettingsService.updateNavigationCollapsed(currentUser.getUserIdComposite(), dto.navigationCollapsed());
        }
        return ResponseEntity.noContent().build();
    }
}
