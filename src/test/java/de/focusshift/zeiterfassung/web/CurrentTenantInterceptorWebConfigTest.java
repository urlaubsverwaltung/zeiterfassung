package de.focusshift.zeiterfassung.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CurrentTenantInterceptorWebConfigTest {

    @InjectMocks
    private CurrentTenantInterceptorWebConfig sut;

    @Mock
    private CurrentTenantInterceptor currentTenantInterceptor;

    @Test
    void ensureThatCurrentTenantInterceptorWasAdded() {

        final InterceptorRegistry registry = mock(InterceptorRegistry.class);
        sut.addInterceptors(registry);

        verify(registry).addInterceptor(currentTenantInterceptor);
    }

    @Test
    void ensureOrderAnnotationIsPresentSoTenantClearRunsLastInPostHandle() {

        final Order order = CurrentTenantInterceptorWebConfig.class.getAnnotation(Order.class);

        assertThat(order).isNotNull();
        assertThat(order.value()).isEqualTo(1);
    }
}
