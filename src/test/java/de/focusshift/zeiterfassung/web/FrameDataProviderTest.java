package de.focusshift.zeiterfassung.web;

import de.focusshift.zeiterfassung.ControllerTest;
import de.focusshift.zeiterfassung.security.SecurityRole;
import de.focusshift.zeiterfassung.web.html.AriaCurrent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_SETTINGS_GLOBAL;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_USER;
import static de.focusshift.zeiterfassung.security.SecurityRole.ZEITERFASSUNG_WORKING_TIME_EDIT_ALL;
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
        @GetMapping({"/test-endpoint", "/timeentries", "/report", "/report/{year}/{month}", "/users", "/users/{id}", "/users/{id}/edit", "/users/search", "/settings", "/settings/{id}", "/settings/about"})
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
                .with(oidcSubject(anyUserIdComposite(), List.of(ZEITERFASSUNG_USER)))
        )
            .andExpect(status().isOk())
            .andExpect(model().attribute("lang", notNullValue()))
            .andExpect(model().attribute("menuHelpUrl", "/help-url"))
            .andExpect(model().attribute("navigation", notNullValue()))
            .andExpect(model().attribute("currentRequestURI", "/test-endpoint"))
            .andExpect(model().attribute("signedInUser", notNullValue()));
    }

    @Test
    void ensureModelAttributesAreNotSetForUnknownUser() throws Exception {

        perform(
            get("/test-endpoint")
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeDoesNotExist("lang"))
            .andExpect(model().attributeDoesNotExist("menuHelpUrl"))
            .andExpect(model().attributeDoesNotExist("navigation"))
            .andExpect(model().attributeDoesNotExist("currentRequestURI"))
            .andExpect(model().attributeDoesNotExist("signedInUser"));
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

    @Test
    void ensureSignedInUserContainsCorrectAttributes() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        final SignedInUserDto signedInUser = (SignedInUserDto) result.getModelAndView().getModel().get("signedInUser");
        assertThat(signedInUser).isNotNull();
        assertThat(signedInUser.id()).isGreaterThan(0);
        assertThat(signedInUser.fullName()).isEqualTo("Some Name");
        assertThat(signedInUser.initials()).isEqualTo("SN");
    }

    @Test
    void ensureLangAttributeIsSet() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        assertThat(result.getModelAndView().getModel()).containsEntry("lang", "en");
    }

    @Test
    void ensureHeaderRefererAttributeIsSet() throws Exception {

        final MvcResult result = perform(
            get("/test-endpoint")
                .with(oidcSubject(anyUserIdComposite(), List.of(ZEITERFASSUNG_USER)))
                .header("Referer", "https://example.com/previous-page")
        )
            .andReturn();

        assertThat(result.getModelAndView().getModel()).containsEntry("header_referer", "https://example.com/previous-page");
    }

    @Test
    void ensureHeaderRefererAttributeIsNullWhenNotPresent() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        assertThat(result.getModelAndView().getModel()).containsEntry("header_referer", null);
    }

    // ==================== Navigation Active State Tests ====================

    @Test
    void ensureTimeentriesLinkIsActiveOnTimeentriesPath() throws Exception {

        final MvcResult result = perform("/timeentries", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto timeentriesLink = items.stream()
            .filter(item -> "/timeentries".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(timeentriesLink.isActive()).isTrue();
        assertThat(timeentriesLink.ariaCurrent()).isEqualTo(AriaCurrent.PAGE.getValue());
    }

    @Test
    void ensureTimeentriesLinkIsNotActiveOnOtherPaths() throws Exception {

        final MvcResult result = perform("/report", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto timeentriesLink = items.stream()
            .filter(item -> "/timeentries".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(timeentriesLink.isActive()).isFalse();
        assertThat(timeentriesLink.ariaCurrent()).isEqualTo(AriaCurrent.FALSE.getValue());
    }

    @Test
    void ensureReportLinkIsActiveOnReportPath() throws Exception {

        final MvcResult result = perform("/report", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto reportLink = items.stream()
            .filter(item -> "/report".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(reportLink.isActive()).isTrue();
        assertThat(reportLink.ariaCurrent()).isEqualTo(AriaCurrent.PAGE.getValue());
    }

    @Test
    void ensureReportLinkIsActiveOnReportSubPaths() throws Exception {

        final MvcResult result = perform("/report/2024/01", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto reportLink = items.stream()
            .filter(item -> "/report".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(reportLink.isActive()).isTrue();
        assertThat(reportLink.ariaCurrent()).isEqualTo(AriaCurrent.PAGE.getValue());
    }

    @Test
    void ensureReportLinkIsNotActiveOnOtherPaths() throws Exception {

        final MvcResult result = perform("/timeentries", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto reportLink = items.stream()
            .filter(item -> "/report".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(reportLink.isActive()).isFalse();
        assertThat(reportLink.ariaCurrent()).isEqualTo(AriaCurrent.FALSE.getValue());
    }

    @ParameterizedTest
    @CsvSource({
        "/users",
        "/users/1",
        "/users/1/edit",
        "/users/search"
    })
    void ensureUsersLinkIsActiveOnUsersPaths(String url) throws Exception {

        final List<SecurityRole> rolesWithUserAccess = List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL);
        final MvcResult result = perform(url, rolesWithUserAccess);

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto usersLink = items.stream()
            .filter(item -> "/users".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(usersLink.isActive()).isTrue();
        assertThat(usersLink.ariaCurrent()).isEqualTo(AriaCurrent.PAGE.getValue());
    }

    @Test
    void ensureUsersLinkIsNotActiveOnOtherPaths() throws Exception {

        final List<SecurityRole> rolesWithUserAccess = List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_WORKING_TIME_EDIT_ALL);
        final MvcResult result = perform("/timeentries", rolesWithUserAccess);

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto usersLink = items.stream()
            .filter(item -> "/users".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(usersLink.isActive()).isFalse();
        assertThat(usersLink.ariaCurrent()).isEqualTo(AriaCurrent.FALSE.getValue());
    }

    @ParameterizedTest
    @CsvSource({
        "/settings",
        "/settings/1",
        "/settings/about"
    })
    void ensureSettingsLinkIsActiveOnSettingsPaths(String url) throws Exception {

        final List<SecurityRole> rolesWithSettingsAccess = List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_SETTINGS_GLOBAL);
        final MvcResult result = perform(url, rolesWithSettingsAccess);

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto settingsLink = items.stream()
            .filter(item -> "/settings".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(settingsLink.isActive()).isTrue();
        assertThat(settingsLink.ariaCurrent()).isEqualTo(AriaCurrent.PAGE.getValue());
    }

    @Test
    void ensureSettingsLinkIsNotActiveOnOtherPaths() throws Exception {

        final List<SecurityRole> rolesWithSettingsAccess = List.of(ZEITERFASSUNG_USER, ZEITERFASSUNG_SETTINGS_GLOBAL);
        final MvcResult result = perform("/timeentries", rolesWithSettingsAccess);

        final List<NavigationItemDto> items = navigationDto(result).items();
        final NavigationItemDto settingsLink = items.stream()
            .filter(item -> "/settings".equals(item.getHref()))
            .findFirst()
            .orElseThrow();

        assertThat(settingsLink.isActive()).isFalse();
        assertThat(settingsLink.ariaCurrent()).isEqualTo(AriaCurrent.FALSE.getValue());
    }

    // ==================== Navigation Item Properties Tests ====================

    @Test
    void ensureNavigationItemsHaveCorrectIds() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();

        assertThat(items.get(0).getId()).isEqualTo("main-navigation-link-timeentries");
        assertThat(items.get(1).getId()).isEqualTo("main-navigation-link-reports");
    }

    @Test
    void ensureNavigationItemsHaveCorrectMessageKeys() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();

        assertThat(items.get(0).getMessageKey()).isEqualTo("navigation.main.timetrack");
        assertThat(items.get(1).getMessageKey()).isEqualTo("navigation.main.reports");
    }

    @Test
    void ensureNavigationItemsHaveCorrectDataTestIds() throws Exception {

        final MvcResult result = perform("/test-endpoint", List.of(ZEITERFASSUNG_USER));

        final List<NavigationItemDto> items = navigationDto(result).items();

        assertThat(items.get(0).getDataTestId()).isEqualTo("navigation-link-timeentries");
        assertThat(items.get(1).getDataTestId()).isEqualTo("navigation-link-reports");
    }

    private NavigationDto navigationDto(MvcResult result) {
        return (NavigationDto) result.getModelAndView().getModel().get("navigation");
    }

    private MvcResult perform(String url, List<SecurityRole> roles) throws Exception {
        return perform(get(url).with(oidcSubject(anyUserIdComposite(), roles)))
            .andExpect(status().isOk())
            .andReturn();
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(new DummyController())
            .addInterceptors(sut)
            .addFilters(new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build()
            .perform(builder);
    }
}
