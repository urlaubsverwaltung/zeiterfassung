package de.focusshift.zeiterfassung.api.timeentry;

import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeentry.TimeEntry;
import de.focusshift.zeiterfassung.timeentry.TimeEntryId;
import de.focusshift.zeiterfassung.timeentry.TimeEntryService;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timeentries")
@PreAuthorize("hasAuthority('ZEITERFASSUNG_API_ACCESS')")
public class TimeEntryApiController {

    private final TimeEntryService timeEntryService;

    public TimeEntryApiController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }

    @GetMapping
    public ResponseEntity<List<TimeEntryApiResponse>> getTimeEntries(
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @AuthenticationPrincipal CurrentOidcUser currentUser
    ) {
        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        final LocalDate queryFrom = from != null ? from : LocalDate.now().minusMonths(1);
        final LocalDate queryTo = to != null ? to : LocalDate.now().plusDays(1);

        final List<TimeEntry> entries = timeEntryService.getEntries(queryFrom, queryTo, userLocalId);
        final List<TimeEntryApiResponse> responses = entries.stream()
            .map(this::toApiResponse)
            .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeEntryApiResponse> getTimeEntry(
        @PathVariable Long id,
        @AuthenticationPrincipal CurrentOidcUser currentUser
    ) {
        final TimeEntryId timeEntryId = new TimeEntryId(id);
        return timeEntryService.findTimeEntry(timeEntryId)
            .filter(te -> te.userIdComposite().equals(currentUser.getUserIdComposite()))
            .map(this::toApiResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TimeEntryApiResponse> createTimeEntry(
        @Valid @RequestBody TimeEntryApiRequest request,
        @AuthenticationPrincipal CurrentOidcUser currentUser
    ) {
        final UserLocalId userLocalId = currentUser.getUserIdComposite().localId();
        final TimeEntry created = timeEntryService.createTimeEntry(
            userLocalId,
            request.comment(),
            request.start(),
            request.end(),
            request.isBreak()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toApiResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimeEntryApiResponse> updateTimeEntry(
        @PathVariable Long id,
        @Valid @RequestBody TimeEntryApiRequest request,
        @AuthenticationPrincipal CurrentOidcUser currentUser
    ) {
        final TimeEntryId timeEntryId = new TimeEntryId(id);
        return timeEntryService.findTimeEntry(timeEntryId)
            .filter(te -> te.userIdComposite().equals(currentUser.getUserIdComposite()))
            .map(timeEntry -> {
                try {
                    final TimeEntry updated = timeEntryService.updateTimeEntry(
                        timeEntryId,
                        request.comment(),
                        request.start(),
                        request.end(),
                        null,
                        request.isBreak()
                    );
                    return ResponseEntity.ok(toApiResponse(updated));
                } catch (Exception e) {
                    return ResponseEntity.<TimeEntryApiResponse>badRequest().build();
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeEntry(
        @PathVariable Long id,
        @AuthenticationPrincipal CurrentOidcUser currentUser
    ) {
        final TimeEntryId timeEntryId = new TimeEntryId(id);
        return timeEntryService.findTimeEntry(timeEntryId)
            .filter(te -> te.userIdComposite().equals(currentUser.getUserIdComposite()))
            .map(timeEntry -> {
                timeEntryService.deleteTimeEntry(timeEntryId);
                return ResponseEntity.<Void>noContent().build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private TimeEntryApiResponse toApiResponse(TimeEntry timeEntry) {
        return new TimeEntryApiResponse(
            timeEntry.id().value(),
            timeEntry.start(),
            timeEntry.end(),
            timeEntry.workDuration().duration(),
            timeEntry.comment(),
            timeEntry.isBreak()
        );
    }
}
