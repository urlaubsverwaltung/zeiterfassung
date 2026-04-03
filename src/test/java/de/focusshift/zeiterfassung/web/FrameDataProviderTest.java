package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class FrameDataProviderTest implements ControllerTest {

    @Controller
    static class DummyController {
        @GetMapping({"/test-endpoint", "/timeentries", "/users", "/settings"})
        public String handleRequest() {
            return "dummy-view";
        }
    }

    private FrameDataProvider sut;

    @BeforeEach
    void setUp() {
        final MenuProperties.Help help = new MenuProperties.Help();
        help.setUrl("/help-url");

        final MenuProperties menuProperties = new MenuProperties();
        menuProperties.setHelp(help);

        sut = new FrameDataProvider(menuProperties);
    }

    @Test
    void ensureModelAttributesAreSetForAuthenticatedUser() throws Exception {

        perform(
            get("/test-endpoint")
                .with(oidcSubject(anyUserIdComposit(), List.of(ZEITERFASSUNG_USER)))
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("lang", notNullValue()))
            .andExpect(model().attribute("menuHelpUrl", "/help-url"))
            .andExpect(model().attribute("navigation", notNullValue()))
            .andExpect(model().attribute("currentRequestURI", "/test-endpoint"))
            .andExpect(model().attribute("signedInUser", notNullValue()));
    }

    @Test
    void ensureBasicNavigationLinks() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        assertThat(navigationDto(result).items())
            .extracting(NavigationItemDto::getHref)
            .containsExactly("/timeentries", "/report");
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = INCLUDE, names = {
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL", "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL", "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL"})
    void ensureNavigationContainsUserManagementLink(SecurityRole role) throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER, role));

        assertThat(navigationDto(result).items())
            .extracting(NavigationItemDto::getHref)
            .contains("/users");
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = EXCLUDE, names = {
        "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL", "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL", "ZEITERFASSUNG_PERMISSIONS_EDIT_ALL"})
    void ensureNavigationDoesNotContainUserManagementLink(SecurityRole role) throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER, role));

        assertThat(navigationDto(result).items())
            .extracting(NavigationItemDto::getHref)
            .doesNotContain("/users");
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = INCLUDE, names = {
        "ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"})
    void ensureNavigationContainsSettingsLink(SecurityRole role) throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER, role));

        assertThat(navigationDto(result).items())
            .extracting(NavigationItemDto::getHref)
            .contains("/settings");
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRole.class, mode = EXCLUDE, names = {
        "ZEITERFASSUNG_WORKING_TIME_EDIT_GLOBAL", "ZEITERFASSUNG_SETTINGS_GLOBAL"})
    void ensureNavigationDoesNotContainSettingsLink(SecurityRole role) throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER, role));

        assertThat(navigationDto(result).items())
            .extracting(NavigationItemDto::getHref)
            .doesNotContain("/settings");
    }


    private NavigationDto navigationDto(MvcResult result) {
        return (NavigationDto) result.getModelAndView().getModel().get("navigation");
    }

    private MvcResult perform(String url, List<SecurityRole> roles) throws Exception {
        return perform(get(url).with(oidcSubject(anyUserIdComposit(), roles)))
            .andExpect(status().isOk())
            .andReturn();
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(new DummyController())
            .setControllerAdvice(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
