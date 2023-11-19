package de.focusshift.zeiterfassung.usermanagement;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
class OvertimeAccountServiceImpl implements OvertimeAccountService {

    private final OvertimeAccountRepository repository;

    OvertimeAccountServiceImpl(OvertimeAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public OvertimeAccount getOvertimeAccount(UserLocalId userLocalId) {
        return repository.findById(userLocalId.value())
            .map(OvertimeAccountServiceImpl::toOvertimeAccount)
            .orElseGet(() -> defaultOvertimeAccount(userLocalId));
    }

    @Override
    public OvertimeAccount updateOvertimeAccount(UserLocalId userLocalId, boolean isOvertimeAllowed, @Nullable Duration maxAllowedOvertime) {

        final OvertimeAccountEntity entity = repository.findById(userLocalId.value()).orElseGet(OvertimeAccountEntity::new);

        entity.setUserId(userLocalId.value());
        entity.setAllowed(isOvertimeAllowed);
        entity.setMaxAllowedOvertime(Optional.ofNullable(maxAllowedOvertime).map(Duration::toString).orElse(null));

        return toOvertimeAccount(repository.save(entity));
    }

    private static OvertimeAccount defaultOvertimeAccount(UserLocalId userLocalId) {
        return new OvertimeAccount(userLocalId, true);
    }

    private static OvertimeAccount toOvertimeAccount(OvertimeAccountEntity entity) {
        return new OvertimeAccount(new UserLocalId(entity.getUserId()), entity.isAllowed(), toDuration(entity.getMaxAllowedOvertime()));
    }

    private static Duration toDuration(String durationValue) {
        return durationValue == null ? null : Duration.parse(durationValue);
    }
}
