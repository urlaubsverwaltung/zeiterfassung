package de.focusshift.launchpad.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.URL;

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
            new App(new URL("https://first.app.example.org"), "launchpad.app.first", "icon-first"),
            new App(new URL("https://second.app.example.org"), "launchpad.app.second", "icon-second")
        );
    }
}
