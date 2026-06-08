package de.focusshift.zeiterfassung.gitactivity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitOAuthTokenRepository extends JpaRepository<GitOAuthTokenEntity, Long> {

    Optional<GitOAuthTokenEntity> findByPlatformAndUserLocalId(String platform, Long userLocalId);

    Optional<GitOAuthTokenEntity> findByPlatformAndPlatformAccountId(String platform, String platformAccountId);

    List<GitOAuthTokenEntity> findByPlatform(String platform);

    List<GitOAuthTokenEntity> findByUserLocalId(Long userLocalId);

    void deleteByPlatformAndUserLocalId(String platform, Long userLocalId);
}
