package de.focusshift.zeiterfassung.gitactivity;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchViewHelper;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static de.focusshift.zeiterfassung.search.UserSearchViewHelper.USER_SEARCH_QUERY_PARAM;
import static de.focusshift.zeiterfassung.web.HotwiredTurboConstants.TURBO_FRAME_HEADER;

@Controller
@RequestMapping("/settings/git-activity")
@PreAuthorize("hasAnyAuthority('ZEITERFASSUNG_SETTINGS_GLOBAL')")
class GitActivityAdminController implements HasLaunchpad, HasTimeClock, HasUserSearch {

    private final GitActivityPlatformSettingsService platformSettingsService;
    private final GitHubActivityProvider gitHubActivityProvider;
    private final UserSearchViewHelper userSearchViewHelper;

    GitActivityAdminController(GitActivityPlatformSettingsService platformSettingsService,
                                GitHubActivityProvider gitHubActivityProvider,
                                UserSearchViewHelper userSearchViewHelper) {
        this.platformSettingsService = platformSettingsService;
        this.gitHubActivityProvider = gitHubActivityProvider;
        this.userSearchViewHelper = userSearchViewHelper;
    }

    @GetMapping
    String index(Model model) {
        populateModel(model, false);
        return "settings/git-activity";
    }

    @GetMapping(params = USER_SEARCH_QUERY_PARAM, headers = TURBO_FRAME_HEADER)
    org.springframework.web.servlet.ModelAndView userSearchFragment(
        @RequestParam(USER_SEARCH_QUERY_PARAM) String query,
        @CurrentUser CurrentOidcUser currentUser, Model model) {
        return userSearchViewHelper.getSuggestionFragment(query, currentUser, model,
            s -> s.userIdComposite().equals(currentUser.getUserIdComposite())
                ? "/timeentries"
                : "/timeentries/users/%s".formatted(s.userLocalId().value()));
    }

    // ── GitHub ────────────────────────────────────────────────────────────────

    @PostMapping("/github")
    String saveGitHub(@RequestParam(value = "githubAppId", required = false) String appId,
                      @RequestParam(value = "githubPrivateKey", required = false) String privateKey,
                      @RequestParam(value = "githubOrgName", required = false) String orgName,
                      @RequestParam(value = "githubAppName", required = false) String appName,
                      Model model) {
        platformSettingsService.saveGitHubSettings(appId, privateKey, orgName, appName);
        gitHubActivityProvider.invalidateTokenCache();
        populateModel(model, true);
        return "settings/git-activity";
    }

    // ── Bitbucket ─────────────────────────────────────────────────────────────

    @PostMapping("/bitbucket")
    String saveBitbucket(@RequestParam(value = "bitbucketOauthKey", required = false) String oauthKey,
                         @RequestParam(value = "bitbucketOauthSecret", required = false) String oauthSecret,
                         @RequestParam(value = "bitbucketWorkspace", required = false) String workspace,
                         @RequestParam(value = "bitbucketCallbackUrl", required = false) String callbackUrl,
                         Model model) {
        platformSettingsService.saveBitbucketSettings(oauthKey, oauthSecret, workspace, callbackUrl);
        populateModel(model, true);
        return "settings/git-activity";
    }

    private void populateModel(Model model, boolean savedSuccess) {
        final GitActivityPlatformSettings gh = platformSettingsService.getGitHubSettings();
        final GitActivityPlatformSettings bb = platformSettingsService.getBitbucketSettings();

        model.addAttribute("githubSettings", gh);
        model.addAttribute("bitbucketSettings", bb);
        model.addAttribute("githubRateLimitPercent", gitHubActivityProvider.getRateLimitPercent());
        model.addAttribute("githubRateLimitRemaining", gitHubActivityProvider.getRateLimitRemaining());
        model.addAttribute("githubRateLimitTotal", gitHubActivityProvider.getRateLimitTotal());
        model.addAttribute("savedSuccess", savedSuccess);
    }
}
