package de.focusshift.zeiterfassung.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebConfigurationTest {

    @InjectMocks
    private WebConfiguration sut;

    @Mock
    private AuthoritiesModelProvider authoritiesModelProvider;
    @Mock
    private DoubleFormatter doubleFormatter;
    @Mock
    private CurrentTenantInterceptor currentTenantInterceptor;

    @Test
    void ensureThatAuthoritiesModelProviderWasAdded() {

        final InterceptorRegistry registry = mock(InterceptorRegistry.class);
        sut.addInterceptors(registry);

        verify(registry).addInterceptor(authoritiesModelProvider);
    }

    @Test
    void ensureThatCurrentTenantInterceptorWasAdded() {

        final InterceptorRegistry registry = mock(InterceptorRegistry.class);
        sut.addInterceptors(registry);

        verify(registry).addInterceptor(currentTenantInterceptor);
    }

    @Test
    void ensureThatDoubleFormatterWasAdded() {

        final FormatterRegistry registry = mock(FormatterRegistry.class);
        sut.addFormatters(registry);

        verify(registry).addFormatter(doubleFormatter);
    }
}
