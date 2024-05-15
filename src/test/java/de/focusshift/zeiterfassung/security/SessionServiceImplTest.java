package de.focusshift.zeiterfassung.security;

import de.focusshift.zeiterfassung.tenancy.user.EMailAddress;
import de.focusshift.zeiterfassung.user.UserId;
import de.focusshift.zeiterfassung.user.UserIdComposite;
import de.focusshift.zeiterfassung.usermanagement.User;
import de.focusshift.zeiterfassung.usermanagement.UserLocalId;
import de.focusshift.zeiterfassung.usermanagement.UserManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    private SessionServiceImpl<Session> sut;

    @Mock
    private FindByIndexNameSessionRepository<Session> sessionRepository;
    @Mock
    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        sut = new SessionServiceImpl<>(sessionRepository, userManagementService);
    }

    @Test
    void markSessionToReloadAuthorities() {

        final String username = "username";
        when(sessionRepository.findByPrincipalName(username))
            .thenReturn(Map.of("65266d07-2ab0-400b-86b5-4b609e552399", new MapSession()));

        sut.markSessionToReloadAuthorities(new UserId(username));

        final ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        final Session session = captor.getValue();
        assertThat((Boolean) session.getAttribute("reloadAuthorities")).isTrue();
    }

    @Test
    void markSessionToReloadAuthoritiesWithUserLocalId() {

        final String username = "username";
        final UserLocalId userLocalId = new UserLocalId(42L);

        when(sessionRepository.findByPrincipalName(username))
            .thenReturn(Map.of("65266d07-2ab0-400b-86b5-4b609e552399", new MapSession()));

        when(userManagementService.findUserByLocalId(userLocalId))
            .thenReturn(Optional.of(anyUser(userLocalId, new UserId(username))));

        sut.markSessionToReloadAuthorities(userLocalId);

        final ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());

        final Session session = captor.getValue();
        assertThat((Boolean) session.getAttribute("reloadAuthorities")).isTrue();
    }

    @Test
    void unmarkSessionToReloadAuthorities() {

        final String someSessionId = "SomeSessionId";

        final MapSession mapSession = new MapSession();
        mapSession.setId(someSessionId);
        when(sessionRepository.findById(someSessionId)).thenReturn(mapSession);

        sut.unmarkSessionToReloadAuthorities(someSessionId);

        final ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        final Session session = captor.getValue();
        assertThat((Boolean) session.getAttribute("reloadAuthorities")).isNull();
    }

    private static User anyUser(UserLocalId userLocalId, UserId userId) {
        return new User(
            new UserIdComposite(userId, userLocalId),
            "givenName",
            "familyName",
            new EMailAddress("email@example.org"),
            Set.of()
        );
    }
}
