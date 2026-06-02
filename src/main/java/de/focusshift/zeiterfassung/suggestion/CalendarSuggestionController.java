package de.focusshift.zeiterfassung.suggestion;

import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Returns absence-based time-entry suggestions from the Urlaubsverwaltung calendar
 * for the currently authenticated user on a given date.
 *
 * <p>Example: {@code GET /api/suggestions?date=2026-06-19}
 */
@RestController
@RequestMapping("/api/suggestions")
public class CalendarSuggestionController {

    private final CalendarSuggestionService suggestionService;
    private final UserManagementService userManagementService;

    public CalendarSuggestionController(
        CalendarSuggestionService suggestionService,
        UserManagementService userManagementService
    ) {
        this.suggestionService = suggestionService;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ResponseEntity<List<CalendarSuggestionDto>> getSuggestions(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @CurrentUser CurrentOidcUser currentUser
    ) {
        final UserLocalId localId = currentUser.getUserIdComposite().localId();
        final User user = userManagementService.findUserByLocalId(localId).orElseThrow();

        final List<CalendarSuggestionDto> suggestions =
            suggestionService.getSuggestionsForDate(user.email().value(), date);

        return ResponseEntity.ok(suggestions);
    }
}
