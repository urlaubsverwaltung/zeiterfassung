package de.focusshift.zeiterfassung.email;

import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObservedTransportTest {

    private TestObservationRegistry registry;
    private Transport delegate;
    private ObservedTransport sut;

    @BeforeEach
    void setUp() {
        registry = TestObservationRegistry.create();
        delegate = mock(Transport.class);
        when(delegate.getURLName()).thenReturn(new URLName("smtp", null, -1, null, null, null));
        sut = new ObservedTransport(Session.getInstance(new Properties()), delegate, registry);
    }

    @Test
    void observesConnect() throws Exception {
        sut.connect("smtp.example.org", 587, "user", "secret");

        verify(delegate).connect("smtp.example.org", 587, "user", "secret");
        assertThat(registry)
            .hasSingleObservationThat()
            .hasNameEqualTo("mail.connect")
            .hasContextualNameEqualTo("mail connect")
            .hasLowCardinalityKeyValue("server.address", "smtp.example.org")
            .hasLowCardinalityKeyValue("server.port", "587")
            .hasLowCardinalityKeyValue("network.protocol.name", "smtp")
            .doesNotHaveError()
            .hasBeenStarted()
            .hasBeenStopped();
    }

    @Test
    void observesConnectWithUnknownServer() throws Exception {
        sut.connect(null, -1, null, null);

        assertThat(registry)
            .hasSingleObservationThat()
            .hasNameEqualTo("mail.connect")
            .hasLowCardinalityKeyValue("server.address", "unknown")
            .hasLowCardinalityKeyValue("server.port", "unknown");
    }

    @Test
    void observesFailedConnectAndRethrows() throws Exception {
        final AuthenticationFailedException exception = new AuthenticationFailedException("535 authentication failed");
        doThrow(exception).when(delegate).connect("smtp.example.org", 587, "user", "wrong");

        assertThatThrownBy(() -> sut.connect("smtp.example.org", 587, "user", "wrong")).isSameAs(exception);

        assertThat(registry)
            .hasSingleObservationThat()
            .hasNameEqualTo("mail.connect")
            .hasError(exception)
            .hasBeenStopped();
    }

    @Test
    void observesSend() throws Exception {
        final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        final Address[] recipients = {new InternetAddress("recipient@example.org")};

        sut.sendMessage(message, recipients);

        verify(delegate).sendMessage(any(), any());
        assertThat(registry)
            .hasSingleObservationThat()
            .hasNameEqualTo("mail.send")
            .doesNotHaveError();
    }

    @Test
    void delegatesConnectionState() {
        when(delegate.isConnected()).thenReturn(true, false);

        assertThat(sut.isConnected()).isTrue();
        assertThat(sut.isConnected()).isFalse();
    }
}
