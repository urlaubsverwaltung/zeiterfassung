package de.focusshift.zeiterfassung.apikey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    Optional<ApiKeyEntity> findByKeyHash(String keyHash);

    List<ApiKeyEntity> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE ApiKeyEntity a SET a.lastUsedAt = :lastUsedAt WHERE a.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") Instant lastUsedAt);
}
