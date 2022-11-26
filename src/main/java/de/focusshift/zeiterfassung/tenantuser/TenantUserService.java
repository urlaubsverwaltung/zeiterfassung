package de.focusshift.zeiterfassung.tenantuser;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserService {

    Optional<TenantUser> getUserByUuid(UUID uuid);

    TenantUser createNewUser(UUID uuid, String givenName, String familyName, EMailAddress eMailAddress, Collection<SecurityRoles> authorities);

    TenantUser updateUser(TenantUser user);

    List<TenantUser> findAllUsers();

    Page<TenantUser> findAllUsers(Pageable pageable);

    void deleteUser(Long id);
}
