package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import jakarta.annotation.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
class OvertimeAccountServiceImpl implements OvertimeAccountService {

    private final OvertimeAccountRepository repository;
    private final UserManagementService userManagementService;
    private final ApplicationEventPublisher applicationEventPublisher;

    OvertimeAccountServiceImpl(OvertimeAccountRepository repository, UserManagementService userManagementService,
                               ApplicationEventPublisher applicationEventPublisher) {
        this.repository = repository;
        this.userManagementService = userManagementService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public OvertimeAccount getOvertimeAccount(UserLocalId userLocalId) {

        final User user = findUser(userLocalId);

        return repository.findById(userLocalId.value())
            .map(overtimeAccountEntity -> toOvertimeAccount(overtimeAccountEntity, user.userIdComposite()))
            .orElseGet(() -> defaultOvertimeAccount(user.userIdComposite()));
    }

    @Override
    public Map<UserIdComposite, OvertimeAccount> getAllOvertimeAccounts() {

        final Map<UserIdComposite, OvertimeAccount> overtimeAccountByUserId = repository.findAll().stream()
            .map(overtimeAccountEntity -> {
                final UserId userId = new UserId(overtimeAccountEntity.getUser().getUuid());
                final UserLocalId userLocalId = new UserLocalId(overtimeAccountEntity.getUser().getId());
                final UserIdComposite userIdComposite = new UserIdComposite(userId, userLocalId);
                return toOvertimeAccount(overtimeAccountEntity, userIdComposite);
            })
            .collect(toMap(OvertimeAccount::userIdComposite, identity()));

        // overtimeAccount is optional, the entity does not exist until the first time "save" is clicked
        // therefore fetch all users and fill Map with default overtimeAccounts
        return userManagementService.findAllUsers().stream()
            .collect(
                toMap(
                    User::userIdComposite,
                    entry -> overtimeAccountByUserId.getOrDefault(entry.userIdComposite(), defaultOvertimeAccount(entry.userIdComposite()))
                )
            );
    }

    @Override
    public OvertimeAccount updateOvertimeAccount(UserLocalId userLocalId, boolean isOvertimeAllowed, @Nullable Duration maxAllowedOvertime) {

        final User user = findUser(userLocalId);
        final OvertimeAccountEntity entity = repository.findById(userLocalId.value()).orElseGet(OvertimeAccountEntity::new);

        entity.setUserId(userLocalId.value());
        entity.setAllowed(isOvertimeAllowed);
        entity.setMaxAllowedOvertime(Optional.ofNullable(maxAllowedOvertime).map(Duration::toString).orElse(null));

        final OvertimeAccount overtimeAccount = toOvertimeAccount(repository.save(entity), user.userIdComposite());

        applicationEventPublisher.publishEvent(new OvertimeAccountUpdatedEvent(
            user.userIdComposite(), isOvertimeAllowed, maxAllowedOvertime
        ));

        return overtimeAccount;
    }

    private User findUser(UserLocalId userLocalId) {
        return userManagementService.findUserByLocalId(userLocalId)
            .orElseThrow(() -> new IllegalStateException("expected user=%s to exist. But got nothing.".formatted(userLocalId)));
    }

    private static OvertimeAccount defaultOvertimeAccount(UserIdComposite userIdComposite) {
        return new OvertimeAccount(userIdComposite, true);
    }

    private static OvertimeAccount toOvertimeAccount(OvertimeAccountEntity entity, UserIdComposite userIdComposite) {
        return new OvertimeAccount(userIdComposite, entity.isAllowed(), toDuration(entity.getMaxAllowedOvertime()));
    }

    private static Duration toDuration(String durationValue) {
        return durationValue == null ? null : Duration.parse(durationValue);
    }
}
