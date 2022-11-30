package de.focusshift.zeiterfassung.security.oidc.claimmapper.groups;

import de.focusshift.zeiterfassung.security.oidc.claimmapper.RolesFromClaimMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.focusshift.zeiterfassung.security.SecurityConfigurationProperties.GROUPS;

/**
 * Neutral way of mapping oidc claim and parse application permissions from it.
 */
@Component
@ConditionalOnProperty(value = "zeiterfassung.security.oidc.claim-mapper", havingValue = GROUPS)
public class RolesFromGroupsClaimMapper implements RolesFromClaimMapper {

    private final Converter<String, GrantedAuthority> converter;

    public RolesFromGroupsClaimMapper(Converter<String, GrantedAuthority> converter) {
        this.converter = converter;
    }

    private static List<String> extractFromList(Map<String, Object> myMap, String key) {
        Object roles = myMap.get(key);
        if (roles instanceof List) {
            return (List) roles;
        }
        return Collections.emptyList();
    }

    @Override
    public List<GrantedAuthority> mapClaimToRoles(Map<String, Object> claims) {

        if (!claims.containsKey(GROUPS)) {
            throw new AccessDeniedException(String.format("claim=%s is missing!", GROUPS));
        }

        return extractFromList(claims, GROUPS)
            .stream()
            .map(converter::convert)
            .filter(Objects::nonNull)
            .toList();
    }
}
