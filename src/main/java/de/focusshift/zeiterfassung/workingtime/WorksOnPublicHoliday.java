package de.focusshift.zeiterfassung.workingtime;

import de.focusshift.zeiterfassung.settings.FederalStateSettings;
import jakarta.annotation.Nullable;

import static java.lang.Boolean.TRUE;

/**
 * Defines whether a person works on a public holiday or not, or if the global setting should be used.
 */
public enum WorksOnPublicHoliday {

    /**
     * global settings should be considered. can be obtained by {@linkplain FederalStateSettings#worksOnPublicHoliday()}
     */
    GLOBAL(null),

    /**
     * overrides global settings with `true`
     */
    YES(TRUE),

    /**
     * overrides global settings with `false`
     */
    NO(Boolean.FALSE);

    private final Boolean boolValue;

    WorksOnPublicHoliday(Boolean boolValue) {
        this.boolValue = boolValue;
    }

    /**
     * @return the boolean value for this enum, {@code null} when {@linkplain WorksOnPublicHoliday#GLOBAL}
     */
    @Nullable
    public Boolean asBoolean() {
        return boolValue;
    }

    public boolean isTrue() {
        return YES.equals(this);
    }

    public static WorksOnPublicHoliday fromBoolean(@Nullable Boolean boolValue) {
        return switch (boolValue) {
            case null -> GLOBAL;
            case Boolean b -> TRUE.equals(b) ? YES : NO;
        };
    }
}
