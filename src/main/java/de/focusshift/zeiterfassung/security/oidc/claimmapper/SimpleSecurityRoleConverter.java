package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.SecurityRole;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SimpleSecurityRoleConverter implements Converter<String, GrantedAuthority> {

    @Override
    public GrantedAuthority convert(String source) {
        try {
            return SecurityRole.valueOf(source).authority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
