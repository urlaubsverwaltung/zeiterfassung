package de.focusshift.zeiterfassung.gitactivity;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the Bitbucket OAuth 2.0 "connect your account" flow.
 *
 * <p>Required configuration (application.properties / environment):
 * <pre>
 *   bitbucket.oauth.key      — OAuth consumer key
 *   bitbucket.oauth.secret   — OAuth consumer secret
 *   bitbucket.oauth.callback-url — full callback URL, e.g. https://timesheet.example.com/account/bitbucket/callback
 * </pre>
 *
 * <p>The Bitbucket OAuth consumer must be registered with scopes:
 * {@code account}, {@code pullrequest}, {@code repository}.
 */
@Controller
@RequestMapping("/account/bitbucket")
public class BitbucketOAuthController {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String PLATFORM = "BITBUCKET";
    private static final String SESSION_STATE_KEY = "bitbucket_oauth_state";
    private static final String SESSION_USER_KEY  = "bitbucket_oauth_user_id";

    private final GitOAuthTokenRepository tokenRepository;
    private final GitActivityPlatformSettingsService platformSettingsService;
    private final RestClient restClient;

    BitbucketOAuthController(GitOAuthTokenRepository tokenRepository,
                              GitActivityPlatformSettingsService platformSettingsService) {
        this.tokenRepository = tokenRepository;
        this.platformSettingsService = platformSettingsService;
        this.restClient = RestClient.builder()
            .defaultHeader("User-Agent", "zeiterfassung-bitbucket-oauth")
            .build();
    }

    public boolean isConfigured() {
        return platformSettingsService.getBitbucketSettings().isConfigured();
    }

    @GetMapping("/connect")
    String connect(@CurrentUser CurrentOidcUser currentUser, HttpSession session) {
        final GitActivityPlatformSettings settings = platformSettingsService.getBitbucketSettings();
        if (!settings.isConfigured()) {
            LOG.warn("Bitbucket OAuth not configured in admin settings");
            return "redirect:/account?bitbucketError=not-configured";
        }

        final String state = UUID.randomUUID().toString();
        session.setAttribute(SESSION_STATE_KEY, state);
        session.setAttribute(SESSION_USER_KEY, currentUser.getUserIdComposite().localId().value());

        final String authUrl = "https://bitbucket.org/site/oauth2/authorize"
            + "?client_id=" + settings.appId()
            + "&response_type=code"
            + "&state=" + state;

        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    String callback(@RequestParam(required = false) String code,
                    @RequestParam(required = false) String state,
                    @RequestParam(required = false) String error,
                    HttpSession session) {

        if (error != null) {
            LOG.warn("Bitbucket OAuth denied by user: {}", error);
            return "redirect:/account?bitbucketError=access-denied";
        }

        final String expectedState = (String) session.getAttribute(SESSION_STATE_KEY);
        final Long userLocalId = (Long) session.getAttribute(SESSION_USER_KEY);
        session.removeAttribute(SESSION_STATE_KEY);
        session.removeAttribute(SESSION_USER_KEY);

        if (expectedState == null || !expectedState.equals(state) || userLocalId == null) {
            LOG.warn("Bitbucket OAuth state mismatch or missing session data");
            return "redirect:/account?bitbucketError=state-mismatch";
        }

        if (code == null || code.isBlank()) {
            return "redirect:/account?bitbucketError=no-code";
        }

        try {
            final Map<String, Object> tokenResponse = exchangeCodeForTokens(code);
            final String accessToken  = (String) tokenResponse.get("access_token");
            final String refreshToken = (String) tokenResponse.get("refresh_token");
            final Number expiresIn    = (Number) tokenResponse.get("expires_in");
            final Instant expiresAt   = expiresIn != null
                ? Instant.now().plusSeconds(expiresIn.longValue())
                : null;

            final String accountId = fetchAccountId(accessToken);
            if (accountId == null) {
                return "redirect:/account?bitbucketError=user-fetch-failed";
            }

            upsertToken(userLocalId, accountId, accessToken, refreshToken, expiresAt);
            LOG.info("Bitbucket account {} connected for user {}", accountId, userLocalId);
            return "redirect:/account?bitbucketConnected=true";

        } catch (Exception e) {
            LOG.error("Bitbucket OAuth token exchange failed", e);
            return "redirect:/account?bitbucketError=token-exchange-failed";
        }
    }

    @PostMapping("/disconnect")
    @Transactional
    String disconnect(@CurrentUser CurrentOidcUser currentUser) {
        final Long userLocalId = currentUser.getUserIdComposite().localId().value();
        tokenRepository.deleteByPlatformAndUserLocalId(PLATFORM, userLocalId);
        LOG.info("Bitbucket account disconnected for user {}", userLocalId);
        return "redirect:/account";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForTokens(String code) {
        final GitActivityPlatformSettings settings = platformSettingsService.getBitbucketSettings();
        final String credentials = Base64.getEncoder().encodeToString(
            (settings.appId() + ":" + settings.appSecret()).getBytes(StandardCharsets.UTF_8));
        final String callbackUrl = settings.callbackUrl() != null ? settings.callbackUrl() : "";

        final String body = "grant_type=authorization_code&code=" + code
            + (callbackUrl.isBlank() ? "" : "&redirect_uri=" + callbackUrl);

        return restClient.post()
            .uri("https://bitbucket.org/site/oauth2/access_token")
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    @SuppressWarnings("unchecked")
    private String fetchAccountId(String accessToken) {
        try {
            final Map<String, Object> user = restClient.get()
                .uri("https://api.bitbucket.org/2.0/user")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return user != null ? (String) user.get("account_id") : null;
        } catch (Exception e) {
            LOG.error("Failed to fetch Bitbucket user info", e);
            return null;
        }
    }

    private void upsertToken(Long userLocalId, String accountId,
                              String accessToken, String refreshToken, Instant expiresAt) {
        final GitOAuthTokenEntity token = tokenRepository
            .findByPlatformAndUserLocalId(PLATFORM, userLocalId)
            .orElseGet(() -> {
                final GitOAuthTokenEntity t = new GitOAuthTokenEntity();
                t.setPlatform(PLATFORM);
                t.setUserLocalId(userLocalId);
                return t;
            });
        token.setPlatformAccountId(accountId);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
    }
}
