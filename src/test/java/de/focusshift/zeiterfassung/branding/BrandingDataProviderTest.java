package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class BrandingDataProviderTest {

    @Controller
    static class DummyController {
        @GetMapping("/test-endpoint")
        public String handleRequest() {
            return "dummy-view";
        }

        @GetMapping("/redirect-endpoint")
        public String handleRedirect() {
            return "redirect:/test-endpoint";
        }
    }

    private BrandingDataProvider sut;

    @BeforeEach
    void setUp() {
        sut = new BrandingDataProvider(new BrandingConfigProperties("Custom Name"));
    }

    @Test
    void ensureApplicationNameIsAddedToModel() throws Exception {
        perform(get("/test-endpoint"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("applicationName", "Custom Name"));
    }

    @Test
    void ensureApplicationNameIsNotAddedForRedirectView() throws Exception {
        perform(get("/redirect-endpoint"))
            .andExpect(status().is3xxRedirection())
            .andExpect(model().attributeDoesNotExist("applicationName"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(new DummyController())
            .addInterceptors(sut)
            .build()
            .perform(builder);
    }
}
