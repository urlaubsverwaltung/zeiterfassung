package de.focusshift.zeiterfassung.gitactivity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "git_activity_platform_settings")
public class GitActivityPlatformSettingsEntity {

    @Id
    @Column(name = "platform", length = 20, nullable = false, updatable = false)
    private String platform;

    /** GitHub: App ID. Bitbucket: OAuth consumer key. */
    @Column(name = "app_id", length = 255)
    private String appId;

    /** GitHub: private key PEM content. Bitbucket: OAuth consumer secret. */
    @Column(name = "app_secret", columnDefinition = "TEXT")
    private String appSecret;

    /** GitHub: org login. Bitbucket: workspace slug. GitLab: group URL. */
    @Column(name = "org_name", length = 255)
    private String orgName;

    /** GitHub: app slug (used in install redirect URL). */
    @Column(name = "app_name", length = 255)
    private String appName;

    /** OAuth callback URL (Bitbucket). */
    @Column(name = "callback_url", length = 1000)
    private String callbackUrl;

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
