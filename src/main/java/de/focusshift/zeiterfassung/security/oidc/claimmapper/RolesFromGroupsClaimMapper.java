package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersProperties.GroupClaimMapperProperties;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.SUB;

/**
 * Claim mapper to parse roles from the 'groups' claim.
 *
 * <p>
 * Demo structure:
 * <pre>
 * {
 *  "groups": [
 *    "zeiterfassung_user",
 *    ...
 *  ]
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(value = "zeiterfassung.security.oidc.claim-mappers.group-claim.enabled", havingValue = "true")
public class RolesFromGroupsClaimMapper implements RolesFromClaimMapper {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final RolesFromClaimMapperConverter converter;
    private final RolesFromClaimMappersProperties properties;

    RolesFromGroupsClaimMapper(
        RolesFromClaimMapperConverter converter,
        RolesFromClaimMappersProperties properties
    ) {
        this.converter = converter;
        this.properties = properties;
    }

    @Override
    public List<GrantedAuthority> mapClaimToRoles(final Map<String, Object> claims) {

        final GroupClaimMapperProperties groupClaim = properties.getGroupClaim();

        final String neededResourceAccessRole = ZEITERFASSUNG_USER.name().toLowerCase();
        if (properties.isAuthorityCheckEnabled() && !claims.containsKey(groupClaim.getClaimName())) {
            LOG.error("User with sub '{}' has not required permission '{}' in '{}' to access zeiterfassung! The claim '{}' is missing!", claims.get(SUB), neededResourceAccessRole, groupClaim.getClaimName(), groupClaim.getClaimName());
            throw new MissingClaimAuthorityException(format("User with sub '%s' has not required permission '%s' in '%s' to access zeiterfassung! The claim '%s' is missing!", claims.get(SUB), neededResourceAccessRole, groupClaim.getClaimName(), groupClaim.getClaimName()));
        }

        final List<String> groups = extractRolesFromClaimName(claims, groupClaim.getClaimName());
        if (properties.isAuthorityCheckEnabled() && groups.stream().noneMatch(neededResourceAccessRole::equals)) {
            LOG.error("User with sub '{}' has not required permission '{}' in '{}' to access zeiterfassung!", claims.get(SUB), neededResourceAccessRole, claims.get("groups"));
            throw new MissingClaimAuthorityException(format("User with sub '%s' has not required permission '%s' in '%s' to access zeiterfassung!", claims.get(SUB), neededResourceAccessRole, claims.get("groups")));
        }

        return groups.stream()
            .map(converter::convert)
            .filter(Objects::nonNull)
            .toList();
    }

    private static List<String> extractRolesFromClaimName(Map<String, Object> claims, String claimName) {

        final Object roles = claims.get(claimName);
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }

        return List.of();
    }
}
