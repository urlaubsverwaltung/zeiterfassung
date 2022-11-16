package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SimpleSecurityRolesConverter implements Converter<String, GrantedAuthority> {

    @Override
    public GrantedAuthority convert(String source) {
        try {
            return SecurityRoles.valueOf(source).authority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
