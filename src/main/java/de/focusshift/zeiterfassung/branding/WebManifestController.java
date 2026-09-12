package de.focusshift.zeiterfassung.branding;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Serves the web app manifest with the configured application name.
 */
@RestController
class WebManifestController {

    private static final List<Icon> ICONS = List.of(
        new Icon("/favicons/android-chrome-192x192.png", "192x192", "image/png"),
        new Icon("/favicons/android-chrome-512x512.png", "512x512", "image/png")
    );

    private final BrandingConfigProperties brandingConfigProperties;

    WebManifestController(BrandingConfigProperties brandingConfigProperties) {
        this.brandingConfigProperties = brandingConfigProperties;
    }

    @GetMapping(value = "/site.webmanifest", produces = "application/manifest+json")
    WebManifest webManifest() {
        final String name = brandingConfigProperties.name();
        return new WebManifest(name, name, "/", ICONS, "#ffffff", "#ffffff", "standalone");
    }

    record WebManifest(
        String name,
        @JsonProperty("short_name") String shortName,
        @JsonProperty("start_url") String startUrl,
        List<Icon> icons,
        @JsonProperty("theme_color") String themeColor,
        @JsonProperty("background_color") String backgroundColor,
        String display
    ) {
    }

    record Icon(String src, String sizes, String type) {
    }
}
