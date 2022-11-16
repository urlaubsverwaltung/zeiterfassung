package de.focusshift.zeiterfassung.security.oidc.claimmapper;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;

public interface RolesFromClaimMapper {

    List<GrantedAuthority> mapClaimToRoles(Map<String, Object> claims);
}
