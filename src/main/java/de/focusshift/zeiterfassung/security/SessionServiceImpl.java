package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

@Service
class SessionServiceImpl<S extends Session> implements SessionService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

    static final String RELOAD_AUTHORITIES = "reloadAuthorities";

    private final FindByIndexNameSessionRepository<S> sessionRepository;
    private final UserManagementService userManagementService;

    @Autowired
    SessionServiceImpl(FindByIndexNameSessionRepository<S> sessionRepository, UserManagementService userManagementService) {
        this.sessionRepository = sessionRepository;
        this.userManagementService = userManagementService;
    }

    @Override
    public void markSessionToReloadAuthorities(UserId userId) {
        final Map<String, S> map = sessionRepository.findByPrincipalName(userId.value());
        for (final S session : map.values()) {
            session.setAttribute(RELOAD_AUTHORITIES, true);
            sessionRepository.save(session);
        }
    }

    public void markSessionToReloadAuthorities(UserLocalId userLocalId) {
        userManagementService.findUserByLocalId(userLocalId)
            .map(User::userId)
            .ifPresentOrElse(
                this::markSessionToReloadAuthorities,
                () -> LOG.warn("Could not find user with id {}. Ignore setting session reload hint.", userLocalId)
            );
    }

    @Override
    public void unmarkSessionToReloadAuthorities(String sessionId) {
        final S session = sessionRepository.findById(sessionId);
        session.removeAttribute(RELOAD_AUTHORITIES);
        sessionRepository.save(session);
    }
}
