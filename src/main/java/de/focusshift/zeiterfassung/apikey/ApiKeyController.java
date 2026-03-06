package de.focusshift.zeiterfassung.apikey;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/apikeys")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_API_ACCESS')")
public class ApiKeyController implements HasLaunchpad, HasTimeClock {

    private final ApiKeyService apiKeyService;
    private final UserManagementService userManagementService;

    public ApiKeyController(ApiKeyService apiKeyService, UserManagementService userManagementService) {
        this.apiKeyService = apiKeyService;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public String index(@CurrentUser CurrentOidcUser currentUser, Model model) {
        final User user = userManagementService.findUserByLocalId(currentUser.getUserIdComposite().localId())
            .orElseThrow();

        final List<ApiKey> apiKeys = apiKeyService.findApiKeysByUser(currentUser.getUserIdComposite().localId());

        model.addAttribute("apiKeys", apiKeys);
        model.addAttribute("user", user);

        return "apikeys/index";
    }

    @PostMapping
    public String create(@CurrentUser CurrentOidcUser currentUser,
                        @RequestParam String label,
                        RedirectAttributes redirectAttributes) {
        final User user = userManagementService.findUserByLocalId(currentUser.getUserIdComposite().localId())
            .orElseThrow();

        final ApiKeyCreationResult result = apiKeyService.createApiKey(user, label);

        redirectAttributes.addFlashAttribute("createdApiKey", result.rawKey());
        redirectAttributes.addFlashAttribute("createdApiKeyLabel", label);

        return "redirect:/apikeys";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @CurrentUser CurrentOidcUser currentUser) {
        apiKeyService.deleteApiKey(id);
        return "redirect:/apikeys";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @CurrentUser CurrentOidcUser currentUser) {
        apiKeyService.toggleApiKeyStatus(id);
        return "redirect:/apikeys";
    }
}
