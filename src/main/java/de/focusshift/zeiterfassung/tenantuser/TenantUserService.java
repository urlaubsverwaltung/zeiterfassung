package de.focusshift.zeiterfassung.tenantuser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserService {

    Optional<TenantUser> getUserByUuid(UUID uuid);

    TenantUser createNewUser(UUID uuid, String givenName, String familyName, EMailAddress eMailAddress);

    TenantUser updateUser(TenantUser user);

    List<TenantUser> findAllUsers();

    void deleteUser(Long id);
}
