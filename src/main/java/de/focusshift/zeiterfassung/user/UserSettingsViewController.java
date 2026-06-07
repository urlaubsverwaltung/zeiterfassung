package de.focusshift.zeiterfassung.user;

import de.focus_shift.launchpad.api.HasLaunchpad;
import de.focusshift.zeiterfassung.search.HasUserSearch;
import de.focusshift.zeiterfassung.search.UserSearchUiFragmentSupplier;
import de.focusshift.zeiterfassung.search.UserSuggestionUrlStrategy;
import de.focusshift.zeiterfassung.security.CurrentUser;
import de.focusshift.zeiterfassung.security.oidc.CurrentOidcUser;
import de.focusshift.zeiterfassung.timeclock.HasTimeClock;
import org.slf4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Locale;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Comparator.comparing;
import static org.slf4j.LoggerFactory.getLogger;

@Controller
@RequestMapping("/personal-settings")
class UserSettingsViewController implements HasTimeClock, HasLaunchpad, HasUserSearch {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserSettingsService userSettingsService;
    private final SupportedLocaleService supportedLocaleService;
    private final UserSettingsDtoValidator userSettingsDtoValidator;
    private final UserSearchUiFragmentSupplier defaultUserSearchUiFragmentSupplier;
    private final MessageSource messageSource;

    UserSettingsViewController(
        UserSettingsService userSettingsService,
        SupportedLocaleService supportedLocaleService,
        UserSettingsDtoValidator userSettingsDtoValidator,
        UserSearchUiFragmentSupplier defaultUserSearchUiFragmentSupplier,
        MessageSource messageSource
    ) {
        this.userSettingsService = userSettingsService;
        this.supportedLocaleService = supportedLocaleService;
        this.userSettingsDtoValidator = userSettingsDtoValidator;
        this.defaultUserSearchUiFragmentSupplier = defaultUserSearchUiFragmentSupplier;
        this.messageSource = messageSource;
    }

    @Override
    public UserSuggestionUrlStrategy userSuggestionUrlStrategy() {
        return (suggestion, context) -> {
            final CurrentOidcUser user = context.getUser();
            if (suggestion.userIdComposite().equals(user.getUserIdComposite())) {
                return "/timeentries";
            } else {
                return "/timeentries/users/%s".formatted(suggestion.userLocalId().value());
            }
        };
    }

    @Override
    public UserSearchUiFragmentSupplier userSearchUiFragmentSupplier() {
        return defaultUserSearchUiFragmentSupplier;
    }

    @GetMapping
    ModelAndView userSettings(Model model, Locale locale, @CurrentUser CurrentOidcUser currentOidcUser) {

        final UserSettings userSettings = userSettingsService.getUserSettings(currentOidcUser.getUserIdComposite());
        model.addAttribute("userSettings", userSettingsToDto(userSettings));
        model.addAttribute("supportedLocales", getSupportedLocales());
        model.addAttribute("supportedThemes", getAvailableThemeDtos(locale));

        return new ModelAndView("user/user-settings", model.asMap());
    }

    @PostMapping
    ModelAndView updateUserSettings(Model model, @ModelAttribute UserSettingsDto userSettingsDto,
                                    Errors errors, Locale locale, @CurrentUser CurrentOidcUser currentOidcUser) {

        final UserIdComposite userIdComposite = currentOidcUser.getUserIdComposite();

        userSettingsDtoValidator.validate(userSettingsDto, errors);
        if (errors.hasErrors()) {
            model.addAttribute("userSettings", userSettingsDto);
            model.addAttribute("supportedLocales", getSupportedLocales());
            model.addAttribute("supportedThemes", getAvailableThemeDtos(locale));
            return new ModelAndView("user/user-settings", model.asMap());
        }

        final Theme theme = themeNameToTheme(userSettingsDto.theme());
        final Locale userLocale = userSettingsDto.locale();
        userSettingsService.updateUserPreference(userIdComposite, theme, userLocale);

        return new ModelAndView("redirect:/personal-settings");
    }

    private LocaleDto toLocaleDto(Locale locale) {
        final String displayName = i18n(locale, "locale");
        return new LocaleDto(locale, displayName);
    }

    private UserSettingsDto userSettingsToDto(UserSettings userSettings) {
        final Locale locale = userSettings.locale().orElse(null);
        return new UserSettingsDto(userSettings.theme().name(), locale);
    }

    private List<ThemeDto> getAvailableThemeDtos(Locale locale) {
        return List.of(
            themeToThemeDto(Theme.SYSTEM, locale),
            themeToThemeDto(Theme.LIGHT, locale),
            themeToThemeDto(Theme.DARK, locale)
        );
    }

    private Theme themeNameToTheme(String themeName) {
        try {
            return Theme.valueOf(themeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOG.error("tried to map unknown name={} to Theme.", themeName, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "theme does not exist.");
        }
    }

    private ThemeDto themeToThemeDto(Theme theme, Locale locale) {
        final String label = i18n(locale, "user-settings.theme." + theme.name());
        return new ThemeDto(theme.name(), label);
    }

    private List<LocaleDto> getSupportedLocales() {
        return supportedLocaleService.getSupportedLocales().stream()
            .map(this::toLocaleDto)
            .sorted(comparing(LocaleDto::getDisplayName))
            .toList();
    }

    private String i18n(Locale locale, String messageKey) {
        return messageSource.getMessage(messageKey, new Object[]{}, locale);
    }
}
