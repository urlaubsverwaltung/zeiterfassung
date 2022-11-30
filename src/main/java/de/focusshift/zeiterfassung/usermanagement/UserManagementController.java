package de.focusshift.zeiterfassung.usermanagement;

import de.focusshift.zeiterfassung.security.SecurityRoles;
import de.focusshift.zeiterfassung.web.html.PaginationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static de.focusshift.zeiterfassung.security.SecurityRoles.*;
import static de.focusshift.zeiterfassung.security.SecurityRules.ALLOW_EDIT_AUTHORITIES;
import static de.focusshift.zeiterfassung.web.html.PaginationPageLinkBuilder.buildPageLinkPrefix;
import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/users")
@PreAuthorize(ALLOW_EDIT_AUTHORITIES)
class UserManagementController {

    private final UserManagementService userManagementService;

    UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    String users(Pageable pageable, Model model) {

        final Page<User> page = userManagementService.findAllUsers(pageable);
        final List<UserDto> userDtos = page.map(UserManagementController::userToDto).toList();
        final PageImpl<UserDto> dtoPage = new PageImpl<>(userDtos, page.getPageable(), page.getTotalElements());

        final String pageLinkPrefix = buildPageLinkPrefix(dtoPage.getPageable());
        final PaginationDto<UserDto> usersPagination = new PaginationDto<>(dtoPage, pageLinkPrefix);
        model.addAttribute("usersPagination", usersPagination);
        model.addAttribute("paginationPageNumbers", IntStream.rangeClosed(1, dtoPage.getTotalPages()).boxed().collect(toList()));

        return "usermanagement/users";
    }

    static UserDto userToDto(User user) {
        final UserAccountAuthoritiesDto authorities = toUserAccountAuthoritiesDto(user.authorities());
        return new UserDto(user.localId().value(), user.givenName(), user.familyName(), fullName(user), user.email().value(), authorities);
    }

    static UserAccountAuthoritiesDto toUserAccountAuthoritiesDto(Collection<SecurityRoles> authorities) {
        return UserAccountAuthoritiesDto.builder()
            .user(authorities.contains(ZEITERFASSUNG_USER))
            .viewReportAll(authorities.contains(ZEITERFASSUNG_VIEW_REPORT_ALL))
            .editAuthorities(authorities.contains(ZEITERFASSUNG_EDIT_AUTHORITIES))
            .build();
    }

    static String fullName(User user) {
        return user.givenName() + " " + user.familyName();
    }
}
