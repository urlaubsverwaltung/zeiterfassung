package de.focusshift.zeiterfassung.absence;

import de.focusshift.zeiterfassung.CachedFunction;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.user.UserSettingsProvider;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static de.focusshift.zeiterfassung.absence.AbsenceTypeCategory.SICK;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

@Service
class AbsenceServiceImpl implements AbsenceService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final AbsenceColor ABSENCE_SICK_COLOR = AbsenceColor.RED;

    private final AbsenceRepository absenceRepository;
    private final AbsenceTypeService absenceTypeService;
    private final UserSettingsProvider userSettingsProvider;
    private final UserManagementService userManagementService;
    private final MessageSource messageSource;

    AbsenceServiceImpl(AbsenceRepository absenceRepository,
                       AbsenceTypeService absenceTypeService,
                       UserSettingsProvider userSettingsProvider,
                       UserManagementService userManagementService,
                       MessageSource messageSource) {

        this.absenceRepository = absenceRepository;
        this.absenceTypeService = absenceTypeService;
        this.userSettingsProvider = userSettingsProvider;
        this.userManagementService = userManagementService;
        this.messageSource = messageSource;
    }

    /* TODO is this method ever used?*/
    @Override
    public Map<LocalDate, List<Absence>> findAllAbsences(UserId userId, Instant from, Instant toExclusive) {

        final ZoneId zoneId = userSettingsProvider.zoneId();

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(userId.value()), toExclusive, from);

        final List<Absence> absences = toAbsences(absenceEntities).toList();

        final Map<LocalDate, List<Absence>> absencesByDate = new HashMap<>();

        for (Absence absence : absences) {
            final LocalDate start = LocalDate.ofInstant(absence.startDate(), zoneId);
            final LocalDate endExclusive = LocalDate.ofInstant(absence.endDate(), zoneId).plusDays(1);
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

        final List<User> users = userManagementService.findAllUsersByLocalIds(userLocalIds);

        final List<String> userIdValues = users
            .stream()
            .map(User::userId)
            .map(UserId::value)
            .toList();

        final Map<UserId, UserIdComposite> idCompositeByUserId = users.stream().collect(toMap(User::userId, User::userIdComposite));

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(userIdValues, period.toExclusive, period.from);

        final Map<UserIdComposite, List<Absence>> result = toAbsences(absenceEntities)
            .collect(groupingBy(absence -> idCompositeByUserId.get(absence.userId())));

        // add empty lists for users without absences
        idCompositeByUserId.values().forEach(userIdComposite -> result.computeIfAbsent(userIdComposite, unused -> List.of()));

        return result;
    }

    @Override
    public Map<UserIdComposite, List<Absence>> getAbsencesForAllUsers(LocalDate from, LocalDate toExclusive) {

        final InstantPeriod period = getInstantPeriod(from, toExclusive);

        final List<AbsenceWriteEntity> absenceEntities =
            absenceRepository.findAllByStartDateLessThanAndEndDateGreaterThanEqual(period.toExclusive, period.from);

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

        final List<AbsenceWriteEntity> absenceEntities = absenceRepository.findAllByUserIdInAndStartDateLessThanAndEndDateGreaterThanEqual(List.of(userId.value()), period.toExclusive, period.from);

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

        final Map<Long, AbsenceType> absenceTypeBySourceId = findAbsenceTypes(absenceEntities).stream()
            .collect(toMap(AbsenceType::sourceId, identity()));

        final Function<Locale, String> sickLabel = new CachedFunction<>(this::sickLabel);

        return absenceEntities.stream()
            .map(entity -> toAbsence(entity, absenceTypeBySourceId::get, sickLabel))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private String sickLabel(Locale locale) {
        return messageSource.getMessage("absence.type.category.SICK", null, locale);
    }

    private Optional<Absence> toAbsence(AbsenceWriteEntity entity, LongFunction<AbsenceType> absenceTypeBySourceIdSupplier, Function<Locale, String> sickLabelSupplier) {

        final AbsenceType absenceType;
        if (entity.getType().getCategory().equals(SICK)) {
            absenceType = new AbsenceType(SICK, null, sickLabelSupplier, ABSENCE_SICK_COLOR);
        } else {
            final AbsenceTypeEntityEmbeddable type = entity.getType();
            absenceType = absenceTypeBySourceIdSupplier.apply(type.getSourceId());
            if (absenceType == null) {
                LOG.warn("Ignore Absence id={} since AbsenceType with sourceId={} is unknown. Will be resolved eventually, hopefully.", entity.getId(), type.getSourceId());
                return Optional.empty();
            }
        }

        final DayLength dayLength = entity.getDayLength();
        final Function<Locale, String> label = getAbsenceTypeLabelWithDayLength(dayLength, absenceType);

        if (entity.getOvertimeHours().isPresent()) {
            return Optional.of(new Absence(
                new UserId(entity.getUserId()),
                entity.getStartDate(),
                entity.getEndDate(),
                dayLength,
                label,
                absenceType.color(),
                absenceType.category(),
                entity.getOvertimeHours().get()
            ));
        } else {
            return Optional.of(new Absence(
                new UserId(entity.getUserId()),
                entity.getStartDate(),
                entity.getEndDate(),
                dayLength,
                label,
                absenceType.color(),
                absenceType.category()
            ));
        }
    }

    private Function<Locale, String> getAbsenceTypeLabelWithDayLength(DayLength dayLength, AbsenceType absenceType) {
        return locale -> {
            String label = absenceType.label(locale);
            if (label == null) {
                final Locale fallbackLocale = Locale.of(locale.getLanguage());
                LOG.info("could not resolve label of absenceType={} for locale={}. falling back to {}", absenceType, locale, fallbackLocale);
                label = absenceType.label(fallbackLocale);
                if (label == null) {
                    LOG.info("could not resolve label of absenceType={} for locale={}. falling back to GERMAN", absenceType, fallbackLocale);
                    label = absenceType.label(Locale.GERMAN);
                }
            }
            return messageSource.getMessage("absence.label." + dayLength, new Object[]{label}, locale);
        };
    }

    private List<AbsenceType> findAbsenceTypes(Collection<AbsenceWriteEntity> absenceEntities) {

        final List<Long> absenceTypeSourceIds = absenceEntities.stream()
            .map(entity -> entity.getType().getSourceId())
            .filter(Objects::nonNull) // category SICK does not have a sourceId
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
