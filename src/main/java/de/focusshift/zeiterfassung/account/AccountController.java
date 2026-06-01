package de.focusshift.zeiterfassung.account;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
@RequestMapping("/account")
class AccountController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserSettingsService userSettingsService;
    private final UserSearchViewHelper userSearchViewHelper;
    private final RestClient restClient;

    AccountController(UserSettingsService userSettingsService, UserSearchViewHelper userSearchViewHelper) {
        this.userSettingsService = userSettingsService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung")
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();
    }

    @GetMapping
    ModelAndView account(Model model, @CurrentUser CurrentOidcUser currentOidcUser) {
        final UserSettings userSettings = userSettingsService.getUserSettings(currentOidcUser.getUserIdComposite());
        final String fullName = currentOidcUser.getUserInfo() != null ? currentOidcUser.getUserInfo().getFullName() : currentOidcUser.getName();
        final String email = currentOidcUser.getUserInfo() != null ? currentOidcUser.getUserInfo().getEmail() : null;

        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("githubLogin", userSettings.githubLogin().orElse(""));
        model.addAttribute("savedSuccess", false);

        return new ModelAndView("account/index", model.asMap());
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query, @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> {
                if (suggestion.userIdComposite().equals(currentUser.getUserIdComposite())) {
                    return "/timeentries";
                } else {
                    return "/timeentries/users/%s".formatted(suggestion.userLocalId().value());
                }
            }
        );
    }

    @PostMapping
    ModelAndView saveAccount(@RequestParam(value = "githubLogin", required = false) String githubLogin,
                             @CurrentUser CurrentOidcUser currentOidcUser, Model model) {
        final String trimmed = githubLogin != null ? githubLogin.trim() : null;
        final String toSave = trimmed != null && !trimmed.isEmpty() ? trimmed : null;
        userSettingsService.updateGithubLogin(currentOidcUser.getUserIdComposite(), toSave);

        final String fullName = currentOidcUser.getUserInfo() != null ? currentOidcUser.getUserInfo().getFullName() : currentOidcUser.getName();
        final String email = currentOidcUser.getUserInfo() != null ? currentOidcUser.getUserInfo().getEmail() : null;

        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("githubLogin", toSave != null ? toSave : "");
        model.addAttribute("savedSuccess", true);

        return new ModelAndView("account/index", model.asMap());
    }

    @GetMapping(value = "/github-verify", headers = TURBO_FRAME_HEADER)
    ModelAndView githubVerify(@RequestParam(value = "username", required = false) String username, Model model) {
        if (username == null || username.isBlank()) {
            model.addAttribute("error", "account.github-login.verify.not-found");
            return new ModelAndView("account/github-verify-result", model.asMap());
        }

        final String trimmedUsername = username.trim();
        try {
            final Map<?, ?> response = restClient.get()
                .uri("https://api.github.com/users/{username}", trimmedUsername)
                .retrieve()
                .body(Map.class);

            if (response != null) {
                model.addAttribute("avatarUrl", response.get("avatar_url"));
                model.addAttribute("displayName", response.get("name") != null ? response.get("name") : response.get("login"));
                model.addAttribute("username", response.get("login"));
            } else {
                model.addAttribute("error", "account.github-login.verify.error");
            }
        } catch (HttpClientErrorException.NotFound e) {
            model.addAttribute("error", "account.github-login.verify.not-found");
        } catch (Exception e) {
            LOG.warn("GitHub verification failed for username={}", trimmedUsername, e);
            model.addAttribute("error", "account.github-login.verify.error");
        }

        return new ModelAndView("account/github-verify-result", model.asMap());
    }
}
