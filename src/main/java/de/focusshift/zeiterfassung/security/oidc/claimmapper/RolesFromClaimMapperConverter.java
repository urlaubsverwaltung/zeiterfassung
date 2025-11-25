package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.SecurityRole;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class RolesFromClaimMapperConverter implements Converter<String, GrantedAuthority> {

    @Override
    public GrantedAuthority convert(@NonNull String source) {
        try {
            return SecurityRole.valueOf(source.toUpperCase()).authority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
