package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.SecurityRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("zeiterfassung.security.oidc.claim-mappers")
public class RolesFromClaimMappersProperties {

    /**
     * Activates or deactivates the authority check of the claim mappers, if they are enabled.
     * When the authority check is enabled, then you need to provide the {@link SecurityRole#ZEITERFASSUNG_USER} authority in your iam,
     * otherwise it can be done via the ui in the application.
     */
    private boolean authorityCheckEnabled = true;

    @NotNull
    private ResourceAccessClaimMapperProperties resourceAccessClaim = new ResourceAccessClaimMapperProperties();

    @NotNull
    private GroupClaimMapperProperties groupClaim = new GroupClaimMapperProperties();

    @NotNull
    private FullResourceAccessClaimMapperProperties fullResourceAccessClaim = new FullResourceAccessClaimMapperProperties();



    public boolean isAuthorityCheckEnabled() {
        return authorityCheckEnabled;
    }

    public void setAuthorityCheckEnabled(boolean authorityCheckEnabled) {
        this.authorityCheckEnabled = authorityCheckEnabled;
    }

    public ResourceAccessClaimMapperProperties getResourceAccessClaim() {
        return resourceAccessClaim;
    }

    public void setResourceAccessClaim(ResourceAccessClaimMapperProperties resourceAccessClaim) {
        this.resourceAccessClaim = resourceAccessClaim;
    }

    public GroupClaimMapperProperties getGroupClaim() {
        return groupClaim;
    }

    public void setGroupClaim(GroupClaimMapperProperties groupClaim) {
        this.groupClaim = groupClaim;
    }

    public FullResourceAccessClaimMapperProperties getFullResourceAccessClaim() {
        return fullResourceAccessClaim;
    }

    public void setFullResourceAccessClaim(FullResourceAccessClaimMapperProperties fullResourceAccessClaim) {
        this.fullResourceAccessClaim = fullResourceAccessClaim;
    }

    public static class ResourceAccessClaimMapperProperties {

        private boolean enabled = false;

        @NotEmpty
        private String resourceApp = "zeiterfassung";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getResourceApp() {
            return resourceApp;
        }

        public void setResourceApp(String resourceApp) {
            this.resourceApp = resourceApp;
        }
    }

    public static class GroupClaimMapperProperties {

        private boolean enabled = false;

        @NotEmpty
        private String claimName = "groups";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }
    }

    public static class FullResourceAccessClaimMapperProperties {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
