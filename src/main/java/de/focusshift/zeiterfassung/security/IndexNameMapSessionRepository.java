package de.focusshift.zeiterfassung.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toMap;

class IndexNameMapSessionRepository implements FindByIndexNameSessionRepository<Session> {

    // <SessionId, Session>
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    // <AttributeName, <SessionId, AttributeValue>>
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> indexMap = new ConcurrentHashMap<>();

    @Override
    public Session findById(String id) {
        return sessions.get(id);
    }

    @Override
    public Session createSession() {
        return new MapSession();
    }

    @Override
    public void save(final Session session) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof final OAuth2AuthenticationToken token) {
            final OAuth2User oAuth2User = token.getPrincipal();
            if (oAuth2User instanceof final OidcUser oidcUser) {
                session.setAttribute(PRINCIPAL_NAME_INDEX_NAME, oidcUser.getSubject());
            }
        }

        sessions.put(session.getId(), session);
        for (final String attributeName : session.getAttributeNames()) {
            indexMap.computeIfAbsent(attributeName, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session.getAttribute(attributeName).toString());
        }
    }

    @Override
    public void deleteById(final String id) {
        final Session session = sessions.remove(id);
        if (session != null) {
            for (final String attributeName : session.getAttributeNames()) {
                final ConcurrentHashMap<String, String> index = indexMap.get(attributeName);
                if (index != null) {
                    index.remove(id);
                }
            }
        }
    }

    @Override
    public Map<String, Session> findByIndexNameAndIndexValue(final String indexName, final String indexValue) {
        final ConcurrentHashMap<String, String> index = indexMap.get(indexName);
        if (index != null) {
            return index.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(indexValue))
                    .collect(toMap(Map.Entry::getKey, entry -> sessions.get(entry.getKey())));
        }
        return Map.of();
    }
}
