package de.focusshift.zeiterfassung.account;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.gitactivity.GitActivityPlatformSettings;
import de.focusshift.zeiterfassung.gitactivity.GitActivityPlatformSettingsService;
import de.focusshift.zeiterfassung.gitactivity.GitOAuthTokenRepository;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import de.focusshift.zeiterfassung.user.UserSettings;
import de.focusshift.zeiterfassung.user.UserSettingsService;
import org.slf4j.Logger;
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
    private final GitOAuthTokenRepository gitOAuthTokenRepository;
    private final GitActivityPlatformSettingsService platformSettingsService;
    private final RestClient restClient;

    AccountController(UserSettingsService userSettingsService,
                      UserSearchViewHelper userSearchViewHelper,
                      GitOAuthTokenRepository gitOAuthTokenRepository,
                      GitActivityPlatformSettingsService platformSettingsService) {
        this.userSettingsService = userSettingsService;
        this.userSearchViewHelper = userSearchViewHelper;
        this.gitOAuthTokenRepository = gitOAuthTokenRepository;
        this.platformSettingsService = platformSettingsService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung")
            .defaultHeader("Accept", "application/vnd.github+json")
            .build();
    }

    @GetMapping
    ModelAndView account(Model model, @CurrentUser CurrentOidcUser currentOidcUser) {
        final UserSettings userSettings = userSettingsService.getUserSettings(currentOidcUser.getUserIdComposite());
        final Long userLocalId = currentOidcUser.getUserIdComposite().localId().value();
        populateModel(model, currentOidcUser, userSettings, userLocalId, false);
        return new ModelAndView("account/index", model.asMap());
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    ModelAndView userSearchFragment(@RequestParam(USER_SEARCH_QUERY_PARAM) String query,
                                    @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            suggestion -> suggestion.userIdComposite().equals(currentUser.getUserIdComposite())
                ? "/timeentries"
                : "/timeentries/users/%s".formatted(suggestion.userLocalId().value())
        );
    }

    /**
     * Saves the GitHub username and notification preference.
     * PAT field has been removed — GitHub App installations now cover private/customer repos.
     */
    @PostMapping
    ModelAndView saveAccount(@RequestParam(value = "githubLogin", required = false) String githubLogin,
                             @RequestParam(value = "githubLoginVerified", required = false, defaultValue = "false") boolean verified,
                             @RequestParam(value = "notificationsEnabled", required = false, defaultValue = "false") boolean notificationsEnabled,
                             @CurrentUser CurrentOidcUser currentOidcUser, Model model) {
        final String trimmed = githubLogin != null ? githubLogin.trim() : null;
        final String toSave = trimmed != null && !trimmed.isEmpty() ? trimmed : null;
        final boolean saveVerified = toSave != null && verified;
        userSettingsService.updateGithubLogin(currentOidcUser.getUserIdComposite(), toSave, saveVerified);
        userSettingsService.updateNotificationsEnabled(currentOidcUser.getUserIdComposite(), notificationsEnabled);
        final Long userLocalId = currentOidcUser.getUserIdComposite().localId().value();
        final UserSettings refreshed = userSettingsService.getUserSettings(currentOidcUser.getUserIdComposite());
        populateModel(model, currentOidcUser, refreshed, userLocalId, true);
        return new ModelAndView("account/index", model.asMap());
    }

    // ── GitHub personal installation (customer repos) ─────────────────────────

    @GetMapping("/github/connect")
    String githubConnect(@CurrentUser CurrentOidcUser currentOidcUser) {
        final GitActivityPlatformSettings gh = platformSettingsService.getGitHubSettings();
        if (!gh.isPersonalInstallConfigured()) {
            return "redirect:/account?githubConnectError=not-configured";
        }
        return "redirect:https://github.com/apps/" + gh.appName()
            + "/installations/new?target_type=User";
    }

    /**
     * GitHub App setup URL — GitHub redirects here after the user installs the app.
     * Configure this URL in the GitHub App settings as the "Setup URL".
     */
    @GetMapping("/github/installed")
    String githubInstalled(@RequestParam(value = "installation_id", required = false) Long installationId,
                           @RequestParam(value = "setup_action", required = false, defaultValue = "install") String setupAction,
                           @CurrentUser CurrentOidcUser currentOidcUser) {
        if (installationId == null) {
            return "redirect:/account?githubConnectError=no-installation-id";
        }
        userSettingsService.updateGithubInstallationId(currentOidcUser.getUserIdComposite(), installationId);
        LOG.info("GitHub personal installation {} connected for user {}",
            installationId, currentOidcUser.getUserIdComposite().localId().value());
        return "redirect:/account?githubPersonalConnected=true";
    }

    @PostMapping("/github/disconnect-personal")
    String githubDisconnectPersonal(@CurrentUser CurrentOidcUser currentOidcUser) {
        userSettingsService.updateGithubInstallationId(currentOidcUser.getUserIdComposite(), null);
        return "redirect:/account";
    }

    // ── GitHub username verification (Turbo Frame) ────────────────────────────

    @GetMapping(value = "/github-verify", headers = TURBO_FRAME_HEADER)
    ModelAndView githubVerify(@RequestParam(value = "username", required = false) String username,
                              @CurrentUser CurrentOidcUser currentOidcUser, Model model) {
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
                final String login = (String) response.get("login");
                final String name = response.get("name") != null ? (String) response.get("name") : login;
                final String avatarUrl = (String) response.get("avatar_url");
                model.addAttribute("avatarUrl", avatarUrl);
                model.addAttribute("displayName", name);
                model.addAttribute("username", login);
                model.addAttribute("verified", true);
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

    private void populateModel(Model model, CurrentOidcUser currentOidcUser,
                               UserSettings userSettings, Long userLocalId, boolean savedSuccess) {
        final String fullName = currentOidcUser.getUserInfo() != null
            ? currentOidcUser.getUserInfo().getFullName()
            : currentOidcUser.getName();
        final String email = currentOidcUser.getUserInfo() != null
            ? currentOidcUser.getUserInfo().getEmail()
            : null;

        final GitActivityPlatformSettings gh = platformSettingsService.getGitHubSettings();
        final GitActivityPlatformSettings bb = platformSettingsService.getBitbucketSettings();
        final boolean bitbucketConnected = gitOAuthTokenRepository
            .findByPlatformAndUserLocalId("BITBUCKET", userLocalId).isPresent();

        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("githubLogin", userSettings.githubLogin().orElse(""));
        model.addAttribute("githubLoginVerified", userSettings.githubLoginVerified());
        model.addAttribute("notificationsEnabled", userSettings.notificationsEnabled());
        // GitHub personal install (customer repos)
        model.addAttribute("githubPersonalInstallConfigured", gh.isPersonalInstallConfigured());
        model.addAttribute("githubInstallationId", userSettings.githubInstallationId().orElse(null));
        // Bitbucket
        model.addAttribute("bitbucketConfigured", bb.isConfigured());
        model.addAttribute("bitbucketConnected", bitbucketConnected);
        model.addAttribute("savedSuccess", savedSuccess);
    }
}
