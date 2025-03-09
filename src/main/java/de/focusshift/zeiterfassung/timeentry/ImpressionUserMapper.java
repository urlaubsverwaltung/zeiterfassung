package de.focusshift.zeiterfassung.timeentry;

import de.focusshift.zeiterfassung.usermanagement.User;

class ImpressionUserMapper {

    private ImpressionUserMapper() {
    }

    public static ImpressionUserDto toImpressionUserDto(User user) {
        return new ImpressionUserDto(user.userLocalId().value(), user.givenName(), user.familyName(),
            user.fullName(), user.email().value());
    }
}
