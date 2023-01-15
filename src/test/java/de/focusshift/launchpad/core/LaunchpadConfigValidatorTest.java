package de.focusshift.launchpad.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.validation.Errors;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaunchpadConfigValidatorTest {

    private LaunchpadConfigValidator sut;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private MessageSource messageSource;

    @Test
    void ensureSupports() {
        sut = new LaunchpadConfigValidator(applicationContext);
        assertThat(sut.supports(LaunchpadConfigProperties.class)).isTrue();
    }

    @Test
    void ensureNonExistentMessageKey() throws Exception {
        sut = new LaunchpadConfigValidator(applicationContext);

        when(applicationContext.getBean(MessageSource.class)).thenReturn(messageSource);
        when(messageSource.getMessage("message-key", new Object[]{}, Locale.GERMAN)).thenThrow(NoSuchMessageException.class);

        final LaunchpadConfigProperties.App e1 = new LaunchpadConfigProperties.App(new URL("http://example.org"), "message-key", "icon");

        final LaunchpadConfigProperties launchpadConfigProperties = new LaunchpadConfigProperties();
        launchpadConfigProperties.setApps(List.of(e1));

        final Errors errors = mock(Errors.class);
        sut.validate(launchpadConfigProperties, errors);

        verify(errors).rejectValue(null, "", "could not find messages key='message-key'");
    }

    @Test
    void ensureMessageKey() throws Exception {
        sut = new LaunchpadConfigValidator(applicationContext);

        when(applicationContext.getBean(MessageSource.class)).thenReturn(messageSource);
        when(messageSource.getMessage("message-key", new Object[]{}, Locale.GERMAN)).thenReturn("translated text");

        final LaunchpadConfigProperties.App e1 = new LaunchpadConfigProperties.App(new URL("http://example.org"), "message-key", "icon");

        final LaunchpadConfigProperties launchpadConfigProperties = new LaunchpadConfigProperties();
        launchpadConfigProperties.setApps(List.of(e1));

        final Errors errors = mock(Errors.class);
        sut.validate(launchpadConfigProperties, errors);

        verifyNoInteractions(errors);
    }
}
