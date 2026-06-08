package de.focusshift.zeiterfassung.gitactivity;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Reads and writes admin-configured credentials for Git platforms.
 * All credential data lives in the {@code git_activity_platform_settings} table —
 * no YAML/environment-variable configuration is required.
 */
@Service
public class GitActivityPlatformSettingsService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    static final String PLATFORM_GITHUB    = "GITHUB";
    static final String PLATFORM_BITBUCKET = "BITBUCKET";
    static final String PLATFORM_GITLAB    = "GITLAB";

    private final GitActivityPlatformSettingsRepository repository;

    GitActivityPlatformSettingsService(GitActivityPlatformSettingsRepository repository) {
        this.repository = repository;
    }

    public GitActivityPlatformSettings getGitHubSettings() {
        return get(PLATFORM_GITHUB);
    }

    public GitActivityPlatformSettings getBitbucketSettings() {
        return get(PLATFORM_BITBUCKET);
    }

    public GitActivityPlatformSettings getGitLabSettings() {
        return get(PLATFORM_GITLAB);
    }

    private GitActivityPlatformSettings get(String platform) {
        return repository.findById(platform)
            .map(e -> new GitActivityPlatformSettings(
                e.getPlatform(), e.getAppId(), e.getAppSecret(),
                e.getOrgName(), e.getAppName(), e.getCallbackUrl()))
            .orElse(GitActivityPlatformSettings.empty(platform));
    }

    public void saveGitHubSettings(String appId, String privateKeyPem,
                                    String orgName, String appName) {
        save(PLATFORM_GITHUB, appId, privateKeyPem, orgName, appName, null);
        LOG.info("GitHub App settings updated — org={} appName={}", orgName, appName);
    }

    public void saveBitbucketSettings(String oauthKey, String oauthSecret,
                                       String workspace, String callbackUrl) {
        save(PLATFORM_BITBUCKET, oauthKey, oauthSecret, workspace, null, callbackUrl);
        LOG.info("Bitbucket OAuth settings updated — workspace={}", workspace);
    }

    public void saveGitLabSettings(String appId, String appSecret,
                                    String groupUrl) {
        save(PLATFORM_GITLAB, appId, appSecret, groupUrl, null, null);
        LOG.info("GitLab settings updated — group={}", groupUrl);
    }

    private void save(String platform, String appId, String appSecret,
                      String orgName, String appName, String callbackUrl) {
        final GitActivityPlatformSettingsEntity entity = repository.findById(platform)
            .orElseGet(() -> {
                final GitActivityPlatformSettingsEntity e = new GitActivityPlatformSettingsEntity();
                e.setPlatform(platform);
                return e;
            });
        entity.setAppId(blank(appId));
        entity.setAppSecret(blank(appSecret));
        entity.setOrgName(blank(orgName));
        entity.setAppName(blank(appName));
        entity.setCallbackUrl(blank(callbackUrl));
        repository.save(entity);
    }

    /** Trims whitespace; returns null for blank values so the DB stays clean. */
    private static String blank(String s) {
        if (s == null) return null;
        final String t = s.strip();
        return t.isEmpty() ? null : t;
    }
}
