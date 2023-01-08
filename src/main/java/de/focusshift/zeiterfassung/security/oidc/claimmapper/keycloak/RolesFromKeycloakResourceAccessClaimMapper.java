package de.focusshift.zeiterfassung.security.oidc.claimmapper.keycloak;

import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static de.focusshift.zeiterfassung.security.SecurityConfigurationProperties.KEYCLOAK;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * This class is aware of roles created for a given oidc client in keycloak
 * and parses the roles of the given user.
 * <p>
 * Demo structure:
 * <pre>
 * {
 *   "resource_access": {
 *     "zeiterfassung": {
 *       "roles": [
 *         "demo_role_from_keycloak"
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
@Component
@ConditionalOnProperty(value = "zeiterfassung.security.oidc.claim-mapper", havingValue = KEYCLOAK)
public class RolesFromKeycloakResourceAccessClaimMapper implements RolesFromClaimMapper {

    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    // TODO make RESOURCE_APP_KEY configurable
    private static final String RESOURCE_APP = "zeiterfassung";
    private static final String ROLES = "roles";

    private final Converter<String, GrantedAuthority> converter;

    public RolesFromKeycloakResourceAccessClaimMapper(Converter<String, GrantedAuthority> converter) {
        this.converter = converter;
    }

    @Override
    public List<GrantedAuthority> mapClaimToRoles(Map<String, Object> claims) {
        return mapClaimToRoles(RESOURCE_APP, claims);
    }

    public List<GrantedAuthority> mapClaimToRoles(String resourceApp, Map<String, Object> claims) {
        final Map<String, Object> resourceAccess = extractFromMap(claims, CLAIM_RESOURCE_ACCESS);
        final Map<String, Object> app = extractFromMap(resourceAccess, resourceApp);

        return extractFromList(app, ROLES)
            .stream()
            .map(converter::convert)
            .toList();
    }

    private Map<String, Object> extractFromMap(Map<String, Object> myMap, String key) {
        final Object inner = myMap.get(key);
        if (inner instanceof Map map) {
            return map;
        }
        return emptyMap();
    }

    private List<String> extractFromList(Map<String, Object> myMap, String key) {
        final Object roles = myMap.get(key);
        if (roles instanceof List list) {
            return list;
        }
        return emptyList();
    }
}
