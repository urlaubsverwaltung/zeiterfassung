package de.focusshift.launchpad.core;

import de.focusshift.launchpad.api.HasLaunchpad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URL;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@SpringBootTest(classes = { LaunchpadControllerAdvice.class })
@ContextConfiguration(classes = { LaunchpadControllerAdviceIT.TestConfig.class })
class LaunchpadControllerAdviceIT {

    @Autowired
    private LaunchpadControllerAdvice sut;

    @Autowired
    private TestConfig.LaunchpadController launchpadController;

    @Autowired
    private TestConfig.OtherController otherController;

    @MockBean
    private LaunchpadServiceImpl launchpadService;

    @Test
    void controllerWithLaunchpad() throws Exception {

        when(launchpadService.getLaunchpad()).thenReturn(
            new Launchpad(
                List.of(
                    new App(new URL("https://example.org"), "message-key", "icon"),
                    new App(new URL("https://example-2.org"), "message-key-2", "icon-2")
                )
            )
        );

        setup(launchpadController)
            .perform(get("/launchpad"))
            .andExpect(model().attribute("launchpad", new LaunchpadDto(
                List.of(
                    new AppDto("https://example.org", "message-key", "icon"),
                    new AppDto("https://example-2.org", "message-key-2", "icon-2")
                )
            )));
    }

    @Test
    void controllerWithoutLaunchpad() throws Exception {

        setup(otherController)
            .perform(get("/no-launchpad"))
            .andExpect(model().attributeDoesNotExist("launchpad"));

        verifyNoInteractions(launchpadService);
    }

    private MockMvc setup(Object controller) {
        return standaloneSetup(controller).setControllerAdvice(sut).build();
    }

    @TestConfiguration
    static class TestConfig {
        @Controller
        @RequestMapping("/launchpad")
        static class LaunchpadController implements HasLaunchpad {
            @GetMapping
            String launchpad() {
                return "some-view";
            }
        }

        @Controller
        @RequestMapping("/no-launchpad")
        static class OtherController {
            @GetMapping
            String noLaunchpad() {
                return "some-view";
            }
        }
    }
}
