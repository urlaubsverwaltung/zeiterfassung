package de.focusshift.zeiterfassung.user;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class UserSettingsService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final UserSettingsRepository userSettingsRepository;
    private final LocaleResolver localeResolver;

    UserSettingsService(UserSettingsRepository userSettingsRepository, LocaleResolver localeResolver) {
        this.userSettingsRepository = userSettingsRepository;
        this.localeResolver = localeResolver;
    }

    Optional<Theme> findTheme(UserIdComposite userIdComposite) {
        final Long localId = userIdComposite.localId().value();
        return userSettingsRepository.findByTenantUserLocalId(localId).map(UserSettingsEntity::getTheme);
    }

    Optional<Locale> getLocale(UserIdComposite userIdComposite) {
        final Long localId = userIdComposite.localId().value();
        return userSettingsRepository.findByTenantUserLocalId(localId).map(UserSettingsEntity::getLocale);
    }

    public UserSettings getUserSettings(UserIdComposite userIdComposite) {
        final UserSettingsEntity entity = findOrGetDefault(userIdComposite);
        return toUserSettings(entity);
    }

    /**
     * Updates the user settings of the person with the given attributes.
     * <p>
     * Also update the browser specific locale based on the given locale
     * and if false with the locale from the request.
     *
     * @param userIdComposite to update the {@link UserSettings} for.
     * @param theme  the {@link Theme} for the person.
     * @return the updated {@link UserSettings}
     */
    UserSettings updateUserPreference(UserIdComposite userIdComposite, Theme theme, @Nullable Locale locale) {
        final UserSettingsEntity entity = findOrGetDefault(userIdComposite);
        entity.setTenantUserLocalId(userIdComposite.localId().value());
        entity.setTheme(theme);
        entity.setLocale(locale);

        final Locale localeFromRequest = locale == null ? getRequest().map(ServletRequest::getLocale).orElse(null) : null;
        entity.setLocaleBrowserSpecific(localeFromRequest);

        final UserSettingsEntity persistedEntity = userSettingsRepository.save(entity);
        LOG.info("Updated user settings to {}", persistedEntity);

        setLocale(persistedEntity.getLocale());

        return toUserSettings(persistedEntity);
    }

    /**
     * Sets the browser specific locale from the request.
     * <p>
     * Only saves the browser specific locale if the saved 'locale' is null.
     * If the saved 'locale' is null, that means, that the localization is based on the browser,
     * and therefore we save it to use it in e-mail templates e.g.
     *
     * @param userIdComposite       to save the browser specific locale
     * @param localeBrowserSpecific browser specific locale
     */
    void updateLocaleBrowserSpecific(UserIdComposite userIdComposite, Locale localeBrowserSpecific) {
        userSettingsRepository.findByTenantUserLocalId(userIdComposite.localId().value())
            .ifPresentOrElse(userSettingsEntity -> {
                if (userSettingsEntity.getLocale() == null) {
                    userSettingsEntity.setLocaleBrowserSpecific(localeBrowserSpecific);
                    userSettingsRepository.save(userSettingsEntity);
                }
            }, () -> {
                final UserSettingsEntity defaultUserSettingsEntity = defaultUserSettingsEntity(userIdComposite);
                defaultUserSettingsEntity.setLocaleBrowserSpecific(localeBrowserSpecific);
                userSettingsRepository.save(defaultUserSettingsEntity);
            });
    }

    private UserSettingsEntity findOrGetDefault(UserIdComposite userIdComposite) {
        final Long id = userIdComposite.localId().value();
        return userSettingsRepository.findById(id).orElseGet(() -> defaultUserSettingsEntity(userIdComposite));
    }

    private UserSettingsEntity defaultUserSettingsEntity(UserIdComposite userIdComposite) {
        final UserSettingsEntity userSettingsEntity = new UserSettingsEntity();
        userSettingsEntity.setTheme(Theme.SYSTEM);
        userSettingsEntity.setTenantUserLocalId(userIdComposite.localId().value());

        LOG.debug("created (not persisted) default userSettingsEntity={}", userSettingsEntity);

        return userSettingsEntity;
    }

    public UserSettings updateGithubLogin(UserIdComposite userIdComposite, @Nullable String githubLogin, boolean verified) {
        final UserSettingsEntity entity = findOrGetDefault(userIdComposite);
        entity.setGithubLogin(githubLogin);
        entity.setGithubLoginVerified(verified);
        final UserSettingsEntity persistedEntity = userSettingsRepository.save(entity);
        LOG.info("Updated github login for user {} verified={}", userIdComposite.localId().value(), verified);
        return toUserSettings(persistedEntity);
    }

    private static UserSettings toUserSettings(UserSettingsEntity userSettingsEntity) {
        return new UserSettings(
            userSettingsEntity.getTheme(),
            userSettingsEntity.getLocale(),
            userSettingsEntity.getLocaleBrowserSpecific(),
            userSettingsEntity.getGithubLogin(),
            userSettingsEntity.isGithubLoginVerified()
        );
    }

    private void setLocale(Locale locale) {
        getRequest().ifPresent(request -> localeResolver.setLocale(request, null, locale));
    }

    private Optional<HttpServletRequest> getRequest() {
        HttpServletRequest request = null;

        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            request = ((ServletRequestAttributes) requestAttributes).getRequest();
        }

        return Optional.ofNullable(request);
    }
}
