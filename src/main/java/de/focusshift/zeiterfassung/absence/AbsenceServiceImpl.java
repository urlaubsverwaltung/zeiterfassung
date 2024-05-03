package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.settings.SupportedLanguages;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantContextHolder;
import de.focusshift.zeiterfassung.tenancy.tenant.TenantId;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Service
class AbsenceServiceImpl implements AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final AbsenceTypeService absenceTypeService;
    private final UserSettingsProvider userSettingsProvider;
    private final TenantContextHolder tenantContextHolder;
    private final UserManagementService userManagementService;
    private final MessageSource messageSource;

    AbsenceServiceImpl(AbsenceRepository absenceRepository,
                       AbsenceTypeService absenceTypeService,
                       UserSettingsProvider userSettingsProvider,
                       TenantContextHolder tenantContextHolder,
                       UserManagementService userManagementService,
                       MessageSource messageSource) {

        this.absenceRepository = absenceRepository;
        this.absenceTypeService = absenceTypeService;
        this.userSettingsProvider = userSettingsProvider;
        this.tenantContextHolder = tenantContextHolder;
        this.userManagementService = userManagementService;
        this.messageSource = messageSource;
    }

    @Override
    public Map<LocalDate, List<Absence>> findAllAbsences(UserId userId, Instant from, Instant toExclusive) {

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, List.of(userId.value()), toExclusive, from);

        final List<Absence> absences = toAbsences(absenceEntities).toList();

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

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, userIdValues, period.toExclusive, period.from);

        final Map<UserIdComposite, List<Absence>> result = toAbsences(absenceEntities)
            .collect(groupingBy(absence -> idCompositeByUserId.get(absence.userId())));

        // add empty lists for users without absences
        idCompositeByUserId.values().forEach(userIdComposite -> result.computeIfAbsent(userIdComposite, unused -> List.of()));

        return result;
    }

    @Override
    public Map<UserIdComposite, List<Absence>> getAbsencesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final InstantPeriod period = getInstantPeriod(from, toExclusive);
        final String tenantId = tenantContextHolder.getCurrentTenantId().orElse(new TenantId("")).tenantId();

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, period.toExclusive, period.from);

        final Map<UserId, List<Absence>> absenceByUserId = toAbsences(absenceEntities)
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

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByTenantIdAndUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(tenantId, List.of(userId.value()), period.toExclusive, period.from);

        return toAbsences(absenceEntities).toList();
    }

    private static void doFromUntil(LocalDate from, LocalDate toExclusive, Consumer<LocalDate> consumer) {
        LocalDate pivot = from;
        while (pivot.isBefore(toExclusive)) {
            consumer.accept(pivot);
            pivot = pivot.plusDays(1);
        }
    }

    private Stream<Absence> toAbsences(List<AbsenceWriteEntity> absenceEntities) {

        final ZoneId zoneId = userSettingsProvider.zoneId();

        final Map<Long, AbsenceType> absenceTypeBySourceId = findAbsenceTypes(absenceEntities).stream()
            .collect(toMap(AbsenceType::sourceId, identity()));

        return absenceEntities.stream()
            .map(entity -> toAbsence(entity, zoneId, absenceTypeBySourceId::get));
    }

    private Absence toAbsence(AbsenceWriteEntity entity, ZoneId zoneId, LongFunction<AbsenceType> absenceTypeBySourceIdSupplier) {

        // fullAbsenceTypeInfo could not be available (is received by message broker VacationTypeUpdated event)
        // the full info contains the correct color and all labels
        // while the AbsenceWriteEntity#getType embeddable contains potentially old info about the color for instance
        final AbsenceType fullAbsenceTypeInfo = absenceTypeBySourceIdSupplier.apply(entity.getType().getSourceId());

        final AbsenceType absenceType = fullAbsenceTypeInfo == null
            ? getAbsenceTypeFromOldEmbeddedInfo(entity)
            : fullAbsenceTypeInfo;

        return new Absence(
            new UserId(entity.getUserId()),
            entity.getStartDate().atZone(zoneId),
            entity.getEndDate().atZone(zoneId),
            entity.getDayLength(),
            absenceType
        );
    }

    private AbsenceType getAbsenceTypeFromOldEmbeddedInfo(AbsenceWriteEntity entity) {

        final AbsenceTypeCategory category = entity.getType().getCategory();
        final Long sourceId = entity.getType().getSourceId();
        final Map<Locale, String> labelByLocale = toEmbeddableAbsenceTypeLabels(entity);
        final AbsenceColor color = entity.getColor();

        return new AbsenceType(category, sourceId, labelByLocale, color);
    }

    private Map<Locale, String> toEmbeddableAbsenceTypeLabels(AbsenceWriteEntity entity) {
        return Stream.of(SupportedLanguages.values())
            .collect(toMap(
                SupportedLanguages::getLocale,
                language -> embeddableAbsenceTypeLabel(entity, language.getLocale())
            ));
    }

    private String embeddableAbsenceTypeLabel(AbsenceWriteEntity entity, Locale locale) {
        final String key = embeddableAbsenceTypeLabelMessageKey(entity);
        return messageSource.getMessage(key, new Object[]{}, locale);
    }

    private String embeddableAbsenceTypeLabelMessageKey(AbsenceWriteEntity entity) {
        final AbsenceTypeEntityEmbeddable type = entity.getType();
        final String dayLength = switch (entity.getDayLength()) {
            case FULL -> "FULL";
            case MORNING -> "MORNING";
            case NOON -> "NOON";
        };
        return "absence.%s.%S.%s".formatted(type.getCategory(), type.getSourceId(), dayLength);
    }

    private List<AbsenceType> findAbsenceTypes(Collection<AbsenceWriteEntity> absenceEntities) {

        final List<Long> absenceTypeSourceIds = absenceEntities.stream()
            .map(AbsenceWriteEntity::getType)
            .map(AbsenceTypeEntityEmbeddable::getSourceId)
            .distinct()
            .toList();

        return absenceTypeService.findAllByAbsenceTypeSourceIds(absenceTypeSourceIds);
    }

    private record InstantPeriod(ZoneId zoneId, Instant from, Instant toExclusive) {}

    private InstantPeriod getInstantPeriod(LocalDate from, LocalDate toExclusive) {

        final ZoneId zoneId = userSettingsProvider.zoneId();
        final Instant fromStartOfDay = Instant.from(from.atStartOfDay().atZone(zoneId));
        final Instant toExclusiveStartOfDay = Instant.from(toExclusive.atStartOfDay().atZone(zoneId));

        return new InstantPeriod(zoneId, fromStartOfDay, toExclusiveStartOfDay);
    }
}
