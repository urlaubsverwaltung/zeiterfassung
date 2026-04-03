package de.focusshift.zeiterfassung.web;

class FrameDataProviderTest {

//    private FrameDataProvider sut;
//
//    @BeforeEach
//    void setUp() {
//        sut = new FrameDataProvider();
//    }
//
//    @AfterEach
//    void tearDown() {
//        SecurityContextHolder.getContext().setAuthentication(null);
//    }
//
//    @ParameterizedTest
//    @EnumSource(value = SecurityRole.class, names = { "ZEITERFASSUNG_WORKING_TIME_EDIT_ALL", "ZEITERFASSUNG_OVERTIME_ACCOUNT_EDIT_ALL" })
//    void ensureShowMainNavigationPersonsIsTrueForAuthority(SecurityRole securityRole) {
//
//        final MockHttpServletRequest request = new MockHttpServletRequest();
//        final MockHttpServletResponse response = new MockHttpServletResponse();
//
//        final OAuth2User oAuth2User = mock(OAuth2User.class);
//        final List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(securityRole.name()));
//        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, authorities, "client-registration-id");
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        final ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("any-view-name");
//
//        sut.postHandle(request, response, new Object(), modelAndView);
//
//        assertThat(modelAndView.getModelMap()).containsEntry("showMainNavigationPersons", true);
//    }
//
//    @ParameterizedTest
//    @EnumSource(value = SecurityRole.class, names = { "ZEITERFASSUNG_USER", "ZEITERFASSUNG_VIEW_REPORT_ALL" })
//    void ensureShowMainNavigationPersonsIsFalseForAuthority(SecurityRole securityRole) {
//
//        final MockHttpServletRequest request = new MockHttpServletRequest();
//        final MockHttpServletResponse response = new MockHttpServletResponse();
//
//        final OAuth2User oAuth2User = mock(OAuth2User.class);
//        final List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(securityRole.name()));
//        final Authentication authentication = new OAuth2AuthenticationToken(oAuth2User, authorities, "client-registration-id");
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        final ModelAndView modelAndView = new ModelAndView();
//        modelAndView.setViewName("any-view-name");
//
//        sut.postHandle(request, response, new Object(), modelAndView);
//
//        assertThat(modelAndView.getModelMap()).containsEntry("showMainNavigationPersons", false);
//    }
}
