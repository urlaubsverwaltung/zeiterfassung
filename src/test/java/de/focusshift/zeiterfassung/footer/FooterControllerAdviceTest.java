package de.focusshift.zeiterfassung.footer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class FooterControllerAdviceTest {

    private FooterControllerAdvice sut;

    @BeforeEach
    void setUp() {
        sut = new FooterControllerAdvice("zeiterfassung-version");
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
        assertThat(modelAndView.getModel()).doesNotContainEntry("version", "zeiterfassung-version");
    }

    @ParameterizedTest
    @ValueSource(strings = {"redirect", "forward"})
    void ensureModelAttributeIsNotSetWhenViewNameStartsWith(String prefix) throws Exception {

        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName(prefix);

        sut.postHandle(null, null, null, modelAndView);
        assertThat(modelAndView.getModel()).doesNotContainEntry("version", "zeiterfassung-version");
    }

    @Test
    void ensureModelAttribute() throws Exception {
        final ModelAndView modelAndView = new ModelAndView("any-viewname");
        sut.postHandle(null, null, null, modelAndView);
        assertThat(modelAndView.getModel()).containsEntry("version", "zeiterfassung-version");
    }
}
