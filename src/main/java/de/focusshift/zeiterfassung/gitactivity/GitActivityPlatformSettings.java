package de.focusshift.zeiterfassung.gitactivity;

/**
 * Immutable snapshot of the admin-configured settings for one Git platform.
 * Retrieved from the database via {@link GitActivityPlatformSettingsService}.
 *
 * <p>Field semantics per platform:
 * <ul>
 *   <li>GitHub:    appId = App ID, appSecret = private key PEM, orgName = org login, appName = app slug</li>
 *   <li>Bitbucket: appId = OAuth consumer key, appSecret = consumer secret, orgName = workspace</li>
 *   <li>GitLab:    appId = OAuth app ID, appSecret = OAuth secret, orgName = group URL</li>
 * </ul>
 */
public record GitActivityPlatformSettings(
    String platform,
    String appId,
    String appSecret,
    String orgName,
    String appName,
    String callbackUrl
) {
    /** True when the minimum credentials (app ID + secret) are present. */
    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }

    /**
     * True when org/workspace/group credentials are also set — enables the
     * server-level installation token path (covers all repos in the org).
     */
    public boolean isOrgConfigured() {
        return isConfigured() && orgName != null && !orgName.isBlank();
    }

    /**
     * True when the app slug is configured — enables the personal-installation
     * redirect URL for users to connect customer repos (GitHub only).
     */
    public boolean isPersonalInstallConfigured() {
        return isConfigured() && appName != null && !appName.isBlank();
    }

    /** Returns an unconfigured placeholder for a given platform. */
    public static GitActivityPlatformSettings empty(String platform) {
        return new GitActivityPlatformSettings(platform, null, null, null, null, null);
    }
}
