package de.focusshift.zeiterfassung.infobanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class InfoBannerControllerAdviceTest {

    private InfoBannerControllerAdvice sut;

    private final InfoBannerConfigProperties infoBannerConfigProperties = new InfoBannerConfigProperties(
        true,
        new InfoBannerConfigProperties.Text("info text")
    );

    @BeforeEach
    void setUp() {
        sut = new InfoBannerControllerAdvice(infoBannerConfigProperties);
    }

    @Test
    void ensureModelAttributeIsNotSetWhenModelAndViewIsNull() {
        assertThatNoException()
            .isThrownBy(() -> sut.postHandle(null, null, null, null));
    }

    @Test
    void ensureModelAttributeIsNotSetWhenModelAndViewHasNoView() throws Exception {
        final ModelAndView modelAndView = new ModelAndView();
        sut.postHandle(null, null, null, modelAndView);
        assertThat(modelAndView.getModel()).doesNotContainEntry("infoBannerText", "info text");
    }

    @ParameterizedTest
    @ValueSource(strings = {"redirect", "forward"})
    void ensureModelAttributeIsNotSetWhenViewNameStartsWith(String prefix) throws Exception {

        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName(prefix);

        sut.postHandle(null, null, null, modelAndView);
        assertThat(modelAndView.getModel()).doesNotContainEntry("infoBannerText", "info text");
    }

    @Test
    void ensureModelAttribute() throws Exception {
        final ModelAndView modelAndView = new ModelAndView("any-viewname");
        sut.postHandle(null, null, null, modelAndView);
        assertThat(modelAndView.getModel()).containsEntry("infoBannerText", "info text");
    }
}
