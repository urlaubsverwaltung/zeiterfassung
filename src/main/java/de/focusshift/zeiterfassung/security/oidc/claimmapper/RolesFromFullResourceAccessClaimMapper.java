package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * Claim mapper to parse roles from 'resource_access' claim under the 'client name'. This mapping is
 * used e.g. in keycloak.
 *
 * <p>
 * Demo structure:
 * <pre>
 * {
 *   …
 *   "resource_access": {
 *     "resource_a": {
 *       "roles": [
 *         "a_user"
 *       ]
 *     },
 *     "zeiterfassung": {
 *       "roles": [
 *         "zeiterfassung_user"
 *       ]
 *     },
 *     "resource_b": {
 *       "roles": [
 *         "b_user",
 *       ]
 *   },
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(value = "zeiterfassung.security.oidc.claim-mappers.full-resource-access-claim.enabled", havingValue = "true")
class RolesFromFullResourceAccessClaimMapper implements RolesFromClaimMapper {

    public static final SimpleGrantedAuthority NEEDED_RESOURCE_ACCESS_ROLE = new SimpleGrantedAuthority(ZEITERFASSUNG_USER.name().toUpperCase());
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private final RolesFromClaimMapperConverter converter;
    private final RolesFromClaimMappersProperties properties;

    RolesFromFullResourceAccessClaimMapper(RolesFromClaimMapperConverter converter,
                                           RolesFromClaimMappersProperties properties) {
        this.converter = converter;
        this.properties = properties;
    }

    @Override
    public List<GrantedAuthority> mapClaimToRoles(Map<String, Object> claims) {

        final String resourceAppIdentifier = properties.getResourceAccessClaim().getResourceApp();

        final Map<String, Object> resourceAccess = extractFromMap(claims, CLAIM_RESOURCE_ACCESS);

        List<GrantedAuthority> grantedAuthorities = resourceAccess.keySet().stream().map(appIdentifier -> {
            final Map<String, Object> resourceApp = extractFromMap(resourceAccess, appIdentifier);
            final List<String> resourceAccessRoles = extractRolesFromResourceApp(resourceApp);

            if (appIdentifier.equals(resourceAppIdentifier)) {
                return resourceAccessRoles.stream().map(converter::convert).filter(Objects::nonNull).toList();
            }

            return resourceAccessRoles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role)).toList();
        }).flatMap(Collection::stream).toList();

        if (properties.isAuthorityCheckEnabled() && grantedAuthorities.stream().noneMatch(NEEDED_RESOURCE_ACCESS_ROLE::equals)) {
            final String requiredRole = NEEDED_RESOURCE_ACCESS_ROLE.toString().toLowerCase();
            throw new MissingClaimAuthorityException(format("User has not required permission '%s' to access zeiterfassung!", requiredRole));
        }

        return grantedAuthorities;
    }

    private Map<String, Object> extractFromMap(Map<String, Object> myMap, String key) {
        final Object inner = myMap.get(key);
        if (inner instanceof Map<?, ?> map) {
            return map.entrySet().stream().collect(toMap(entry -> entry.getKey().toString(), Map.Entry::getValue));
        }
        return Map.of();
    }

    private List<String> extractRolesFromResourceApp(final Map<String, Object> clientInformation) {

        final Object roles = clientInformation.get(ROLES);
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
