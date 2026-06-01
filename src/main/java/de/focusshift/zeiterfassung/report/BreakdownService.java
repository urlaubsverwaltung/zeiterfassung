package de.focusshift.zeiterfassung.report;

import de.focusshift.zeiterfassung.activitytype.ActivityType;
import de.focusshift.zeiterfassung.activitytype.ActivityTypeService;
import de.focusshift.zeiterfassung.project.Project;
import de.focusshift.zeiterfassung.project.ProjectService;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Service
class BreakdownService {

    record ProjectBreakdown(String projectName, Duration duration) {}

    record CustomerBreakdown(String customerName, Duration duration, List<ProjectBreakdown> projects) {}

    record ActivityTypeBreakdown(String name, Duration duration) {}

    record BreakdownResult(
        List<CustomerBreakdown> byCustomer,
        List<ActivityTypeBreakdown> byActivityType,
        Duration total
    ) {
        boolean isEmpty() {
            return total.isZero();
        }
    }

    private static final String NO_PROJECT = "—";
    private static final String NO_CUSTOMER = "No project";
    private static final String NO_ACTIVITY = "No activity";

    private final TimeEntryService timeEntryService;
    private final ProjectService projectService;
    private final ActivityTypeService activityTypeService;

    BreakdownService(TimeEntryService timeEntryService, ProjectService projectService, ActivityTypeService activityTypeService) {
        this.timeEntryService = timeEntryService;
        this.projectService = projectService;
        this.activityTypeService = activityTypeService;
    }

    BreakdownResult breakdown(LocalDate from, LocalDate toExclusive, List<UserLocalId> userLocalIds) {

        final List<TimeEntry> entries = timeEntryService.getEntries(from, toExclusive, userLocalIds)
            .values().stream()
            .flatMap(Collection::stream)
            .filter(e -> !e.isBreak())
            .toList();

        if (entries.isEmpty()) {
            return new BreakdownResult(List.of(), List.of(), Duration.ZERO);
        }

        final Map<Long, Project> projectById = projectService.findAll().stream()
            .collect(toMap(p -> p.id().value(), identity()));
        final Map<Long, ActivityType> activityTypeById = activityTypeService.findAll().stream()
            .collect(toMap(at -> at.id().value(), identity()));

        // Aggregate by customer → project
        final Map<String, Map<String, Duration>> byCustomerProject = new LinkedHashMap<>();
        final Map<String, Duration> byActivity = new LinkedHashMap<>();
        Duration total = Duration.ZERO;

        for (TimeEntry entry : entries) {
            final Duration d = entry.workDuration().durationInMinutes();
            total = total.plus(d);

            // Customer / project
            String customerName = NO_CUSTOMER;
            String projectName = NO_PROJECT;
            if (entry.projectId() != null) {
                final Project project = projectById.get(entry.projectId());
                if (project != null) {
                    customerName = project.customerName();
                    projectName = project.name();
                }
            }
            byCustomerProject.computeIfAbsent(customerName, k -> new LinkedHashMap<>())
                .merge(projectName, d, Duration::plus);

            // Activity type
            String activityName = NO_ACTIVITY;
            if (entry.activityTypeId() != null) {
                final ActivityType at = activityTypeById.get(entry.activityTypeId());
                if (at != null) activityName = at.name();
            }
            byActivity.merge(activityName, d, Duration::plus);
        }

        final List<CustomerBreakdown> customerBreakdowns = byCustomerProject.entrySet().stream()
            .map(e -> {
                final List<ProjectBreakdown> projects = e.getValue().entrySet().stream()
                    .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
                    .map(pe -> new ProjectBreakdown(pe.getKey(), pe.getValue()))
                    .toList();
                final Duration custTotal = projects.stream().map(ProjectBreakdown::duration).reduce(Duration.ZERO, Duration::plus);
                return new CustomerBreakdown(e.getKey(), custTotal, projects);
            })
            .sorted(Comparator.comparing(CustomerBreakdown::duration).reversed())
            .toList();

        final List<ActivityTypeBreakdown> activityBreakdowns = byActivity.entrySet().stream()
            .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
            .map(e -> new ActivityTypeBreakdown(e.getKey(), e.getValue()))
            .toList();

        return new BreakdownResult(customerBreakdowns, activityBreakdowns, total);
    }
}
