package de.focusshift.zeiterfassung.usermanagement;

import org.springframework.stereotype.Service;

import java.time.Duration;

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
    public OvertimeAccount updateOvertimeAccount(OvertimeAccount overtimeAccount) {

        final OvertimeAccountEntity entity = repository.findById(overtimeAccount.getUserLocalId().value()).orElseGet(OvertimeAccountEntity::new);

        entity.setUserId(overtimeAccount.getUserLocalId().value());
        entity.setAllowed(overtimeAccount.isAllowed());
        entity.setMaxAllowedOvertime(overtimeAccount.getMaxAllowedOvertime().map(Duration::toString).orElse(null));

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
