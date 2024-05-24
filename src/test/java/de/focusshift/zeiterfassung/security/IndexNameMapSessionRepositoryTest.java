package de.focusshift.zeiterfassung.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME;

class IndexNameMapSessionRepositoryTest {

    private IndexNameMapSessionRepository sut;

    @BeforeEach
    void setUp() {
        sut = new IndexNameMapSessionRepository();
    }

    @Test
    void createSession() {
        assertThat(sut.createSession()).isInstanceOf(MapSession.class);
    }

    @Test
    void saveAndFindById() {

        final MapSession session = new MapSession("id");

        sut.save(session);

        assertThat(sut.findById("id")).isEqualTo(session);
    }

    @Test
    void saveAndFindByIndexNameAndIndexValue() {

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("user1"));
        final MapSession session1 = new MapSession("id1");
        final MapSession session2 = new MapSession("id2");
        sut.save(session1);
        sut.save(session2);

        context.setAuthentication(prepareOAuth2Authentication("user2"));
        final MapSession session3 = new MapSession("id3");
        sut.save(session3);

        final Map<String, Session> byIndexNameAndIndexValue = sut.findByIndexNameAndIndexValue(PRINCIPAL_NAME_INDEX_NAME, "user1");
        assertThat(byIndexNameAndIndexValue)
                .containsExactlyInAnyOrderEntriesOf(Map.of("id1", session1, "id2", session2))
                .doesNotContain(entry("id3", session3));

        assertThat(sut.findByIndexNameAndIndexValue("unknown-index-name", "unknown-index-value")).isEmpty();
    }

    @Test
    void ensureToNotFindValuesByFindByIndexNameAndIndexValueIfIndexNameIsInWrongSensitivity() {

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("user1"));
        final MapSession session1 = new MapSession("id1");
        session1.setAttribute("SomeAttributeName", "CamelCaseValue");
        sut.save(session1);

        assertThat(sut.findByIndexNameAndIndexValue("SomeAttributeName", "camelcasevalue")).isEmpty();
    }

    @Test
    void deleteById() {

        final SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(prepareOAuth2Authentication("user"));
        final MapSession session = new MapSession("id");

        sut.save(session);

        assertThat(sut.findById("id")).isEqualTo(session);
        assertThat(sut.findByIndexNameAndIndexValue(PRINCIPAL_NAME_INDEX_NAME, "user")).containsExactlyInAnyOrderEntriesOf(Map.of("id", session));

        sut.deleteById(session.getId());

        assertThat(sut.findById("id")).isNull();
        assertThat(sut.findByIndexNameAndIndexValue(PRINCIPAL_NAME_INDEX_NAME, "user")).isEmpty();
    }

    private OAuth2AuthenticationToken prepareOAuth2Authentication(String subject) {

        final DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(),
                OidcIdToken.withTokenValue("token-value").subject(subject).build()
        );

        final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        return authentication;
    }
}
