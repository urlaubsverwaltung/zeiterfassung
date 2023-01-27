package de.focusshift.launchpad.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.URL;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { LaunchpadServiceImpl.class })
@EnableConfigurationProperties(LaunchpadConfigProperties.class)
@TestPropertySource("classpath:launchpad-example.properties")
class LaunchpadServiceImplIT {

    @Autowired
    private LaunchpadServiceImpl sut;

    @Test
    void ensureLaunchpad() throws Exception {

        final Launchpad launchpad = sut.getLaunchpad();

        assertThat(launchpad.apps()).containsExactly(
            new App(new URL("https://first.app.example.org"), new AppName("Anwendung 1", Map.of(Locale.GERMAN, "Anwendung 1", Locale.ENGLISH, "App 1")), "icon-first"),
            new App(new URL("https://second.app.example.org"), new AppName("Anwendung 2", Map.of(Locale.GERMAN, "Anwendung 2", Locale.ENGLISH, "App 2")), "icon-second")
        );
    }
}
