package de.focusshift.zeiterfassung.branding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class WebManifestControllerTest {

    private WebManifestController sut;

    @BeforeEach
    void setUp() {
        sut = new WebManifestController(new BrandingConfigProperties("Custom Name"));
    }

    @Test
    void ensureWebManifestUsesConfiguredApplicationName() throws Exception {

        standaloneSetup(sut).build()
            .perform(get("/site.webmanifest"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/manifest+json"))
            .andExpect(jsonPath("$.name").value("Custom Name"))
            .andExpect(jsonPath("$.short_name").value("Custom Name"))
            .andExpect(jsonPath("$.start_url").value("/"))
            .andExpect(jsonPath("$.display").value("standalone"))
            .andExpect(jsonPath("$.theme_color").value("#ffffff"))
            .andExpect(jsonPath("$.background_color").value("#ffffff"))
            .andExpect(jsonPath("$.icons.length()").value(2))
            .andExpect(jsonPath("$.icons[0].src").value("/favicons/android-chrome-192x192.png"))
            .andExpect(jsonPath("$.icons[0].sizes").value("192x192"))
            .andExpect(jsonPath("$.icons[0].type").value("image/png"))
            .andExpect(jsonPath("$.icons[1].src").value("/favicons/android-chrome-512x512.png"))
            .andExpect(jsonPath("$.icons[1].sizes").value("512x512"))
            .andExpect(jsonPath("$.icons[1].type").value("image/png"));
    }
}
