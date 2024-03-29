package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final UserSettingsProvider userSettingsProvider;
    private final TenantContextHolder tenantContextHolder;
    private final UserManagementService userManagementService;

    AbsenceServiceImpl(AbsenceRepository absenceRepository, UserSettingsProvider userSettingsProvider, TenantContextHolder tenantContextHolder, UserManagementService userManagementService) {
        this.absenceRepository = absenceRepository;
        this.userSettingsProvider = userSettingsProvider;
        this.tenantContextHolder = tenantContextHolder;
        this.userManagementService = userManagementService;
    }

    @Override
    public Map<LocalDate, List<Absence>> findAllAbsences(UserId userId, Instant from, Instant toExclusive) {

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        final List<Absence> absences =
            absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, List.of(userId.value()), toExclusive, from)
                .stream()
                .map(entity -> toAbsence(entity, zoneId))
                .toList();

        final Map<LocalDate, List<Absence>> absencesByDate = new HashMap<>();

        for (Absence absence : absences) {
            final LocalDate start = LocalDate.ofInstant(absence.startDate().toInstant(), zoneId);
            final LocalDate endExclusive = LocalDate.ofInstant(absence.endDate().toInstant(), zoneId).plusDays(1);
            doFromUntil(start, endExclusive, date -> absencesByDate.computeIfAbsent(date, unused -> new ArrayList<>()).add(absence));
        }

        // add empty lists for dates without absences
        final LocalDate fromDate = LocalDate.ofInstant(from, zoneId);
        final LocalDate toDateExclusive = LocalDate.ofInstant(toExclusive, zoneId);
        doFromUntil(fromDate, toDateExclusive, date -> absencesByDate.computeIfAbsent(date, unused -> new ArrayList<>()));

        return absencesByDate;
    }

    @Override
    public Map<UserIdComposite, List<Absence>> getAbsencesByUserIds(List<UserLocalId> userLocalIds, LocalDate from, LocalDate toExclusive) {

        final InstantPeriod period = getInstantPeriod(from, toExclusive);
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);

        final List<String> userIdValues = users
            .stream()
            .map(User::userId)
            .map(UserId::value)
            .toList();

        final Map<UserId, UserIdComposite> idCompositeByUserId = users.stream().collect(toMap(User::userId, User::userIdComposite));

        final Map<UserIdComposite, List<Absence>> result = absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, userIdValues, period.toExclusive, period.from)
            .stream()
            .map(absenceWriteEntity -> toAbsence(absenceWriteEntity, period.zoneId))
            .collect(groupingBy(absence -> idCompositeByUserId.get(absence.userId())));

        // add empty lists for users without absences
        idCompositeByUserId.values().forEach(userIdComposite -> result.computeIfAbsent(userIdComposite, unused -> List.of()));

        return result;
    }

    @Override
    public Map<UserIdComposite, List<Absence>> getAbsencesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final InstantPeriod period = getInstantPeriod(from, toExclusive);
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        final Map<UserId, List<Absence>> absenceByUserId = absenceRepository.findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, period.toExclusive, period.from)
            .stream()
            .map(entity -> toAbsence(entity, period.zoneId))
            .collect(groupingBy(Absence::userId));

        final Map<UserId, UserIdComposite> idCompositeByUserId = userManagementService.findAllUsers().stream()
            .collect(toMap(User::userId, User::userIdComposite));

        final Map<UserIdComposite, List<Absence>> result = absenceByUserId.entrySet()
            .stream()
            .collect(toMap(entry -> idCompositeByUserId.get(entry.getKey()), Map.Entry::getValue));

        // add empty lists for users without absences
        idCompositeByUserId.values().forEach(userIdComposite -> result.computeIfAbsent(userIdComposite, unused -> List.of()));

        return result;
    }

    @Override
    public List<Absence> getAbsencesByUserId(UserId userId, LocalDate from, LocalDate toExclusive) {

        final InstantPeriod period = getInstantPeriod(from, toExclusive);
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        return absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, List.of(userId.value()), period.toExclusive, period.from)
            .stream()
            .map(entity -> toAbsence(entity, period.zoneId))
            .toList();
    }

    private static void doFromUntil(LocalDate from, LocalDate toExclusive, Consumer<LocalDate> consumer) {
        LocalDate pivot = from;
        while (pivot.isBefore(toExclusive)) {
            consumer.accept(pivot);
            pivot = pivot.plusDays(1);
        }
    }

    private static Absence toAbsence(AbsenceWriteEntity entity, ZoneId zoneId) {
        return new Absence(
            new UserId(entity.getUserId()),
            entity.getStartDate().atZone(zoneId),
            entity.getEndDate().atZone(zoneId),
            entity.getDayLength(),
            new AbsenceType(entity.getType().getCategory(), entity.getType().getSourceId()),
            entity.getColor()
        );
    }

    private record InstantPeriod(ZoneId zoneId, Instant from, Instant toExclusive) {}

    private InstantPeriod getInstantPeriod(LocalDate from, LocalDate toExclusive) {

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(zoneId));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(zoneId));

        return new InstantPeriod(zoneId, fromStartOfDay, toExclusiveStartOfDay);
    }
}
