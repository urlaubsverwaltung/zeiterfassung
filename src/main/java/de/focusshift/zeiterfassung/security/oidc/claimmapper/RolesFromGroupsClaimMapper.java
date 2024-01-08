package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMappersProperties.GroupClaimMapperProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.String.format;

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

    private final RolesFromClaimMapperConverter converter;
    private final RolesFromClaimMappersProperties properties;

    RolesFromGroupsClaimMapper(RolesFromClaimMapperConverter converter,
                               RolesFromClaimMappersProperties properties) {
        this.converter = converter;
        this.properties = properties;
    }

    @Override
    public List<GrantedAuthority> mapClaimToRoles(final Map<String, Object> claims) {

        final GroupClaimMapperProperties groupClaim = properties.getGroupClaim();

        final String neededResourceAccessRole = ZEITERFASSUNG_USER.name().toLowerCase();
        if (!claims.containsKey(groupClaim.getClaimName())) {
            throw new MissingClaimAuthorityException(format("User has not required permission '%s' to access zeiterfassung! The claim '%s' is missing!", neededResourceAccessRole, groupClaim.getClaimName()));
        }

        final List<String> groups = extractRolesFromClaimName(claims, groupClaim.getClaimName());
        if (groups.stream().noneMatch(neededResourceAccessRole::equals)) {
            throw new MissingClaimAuthorityException(format("User has not required permission '%s' to access zeiterfassung!", neededResourceAccessRole));
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
