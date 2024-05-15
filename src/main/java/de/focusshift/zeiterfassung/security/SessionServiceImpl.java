package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.user.UserId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class SessionServiceImpl<S extends Session> implements SessionService {

    static final String RELOAD_AUTHORITIES = "reloadAuthorities";

    private final FindByIndexNameSessionRepository<S> sessionRepository;

    @Autowired
    SessionServiceImpl(FindByIndexNameSessionRepository<S> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public void markSessionToReloadAuthorities(UserId userId) {
        final Map<String, S> map = sessionRepository.findByPrincipalName(userId.value());
        for (final S session : map.values()) {
            session.setAttribute(RELOAD_AUTHORITIES, true);
            sessionRepository.save(session);
        }
    }

    @Override
    public void unmarkSessionToReloadAuthorities(String sessionId) {
        final S session = sessionRepository.findById(sessionId);
        session.removeAttribute(RELOAD_AUTHORITIES);
        sessionRepository.save(session);
    }
}
