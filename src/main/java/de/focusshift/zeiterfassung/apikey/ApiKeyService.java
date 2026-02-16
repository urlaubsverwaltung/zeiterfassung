package de.focusshift.zeiterfassung.apikey;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class ApiKeyService {

    private static final int API_KEY_LENGTH = 32;
    private static final String API_KEY_PREFIX = "zf_";

    private final ApiKeyRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ApiKeyService(ApiKeyRepository repository, PasswordEncoder passwordEncoder, Clock clock) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public ApiKeyCreationResult createApiKey(User user, String label) {
        final String rawKey = generateApiKey();
        final String keyHash = passwordEncoder.encode(rawKey);

        final ApiKeyEntity entity = new ApiKeyEntity(
            user.userIdComposite().tenantId().value(),
            user.userIdComposite().localId().value(),
            keyHash,
            label,
            clock.instant()
        );

        repository.save(entity);

        final ApiKey apiKey = toApiKey(entity);
        return new ApiKeyCreationResult(apiKey, rawKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> findApiKeysByUser(UserLocalId userId) {
        return repository.findByUserId(userId.value()).stream()
            .map(this::toApiKey)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ApiKey> findApiKeyById(Long id) {
        return repository.findById(id).map(this::toApiKey);
    }

    @Transactional
    public Optional<User> validateApiKey(String rawKey) {
        if (!rawKey.startsWith(API_KEY_PREFIX)) {
            return Optional.empty();
        }

        final List<ApiKeyEntity> allKeys = repository.findAll();
        for (ApiKeyEntity entity : allKeys) {
            if (entity.isActive() && passwordEncoder.matches(rawKey, entity.getKeyHash())) {
                if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(clock.instant())) {
                    return Optional.empty();
                }

                repository.updateLastUsedAt(entity.getId(), clock.instant());
                return Optional.empty(); // TODO: load user from entity.getUserId()
            }
        }

        return Optional.empty();
    }

    @Transactional
    public void deleteApiKey(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void toggleApiKeyStatus(Long id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setActive(!entity.isActive());
            repository.save(entity);
        });
    }

    private String generateApiKey() {
        final SecureRandom random = new SecureRandom();
        final byte[] bytes = new byte[API_KEY_LENGTH];
        random.nextBytes(bytes);
        return API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ApiKey toApiKey(ApiKeyEntity entity) {
        return new ApiKey(
            entity.getId(),
            new TenantId(entity.getTenantId()),
            new UserLocalId(entity.getUserId()),
            entity.getLabel(),
            entity.getCreatedAt(),
            entity.getLastUsedAt(),
            entity.getExpiresAt(),
            entity.isActive()
        );
    }
}
