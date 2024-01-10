package de.focusshift.zeiterfassung.settings;

import de.focusshift.zeiterfassung.publicholiday.FederalState;
import de.focusshift.zeiterfassung.web.html.HtmlOptgroupDto;
import de.focusshift.zeiterfassung.web.html.HtmlOptionDto;
import de.focusshift.zeiterfassung.web.html.HtmlSelectDto;

import java.util.ArrayList;
import java.util.List;

public class FederalStateSelectDtoFactory {

    private FederalStateSelectDtoFactory() {
        //
    }

    public static HtmlSelectDto federalStateSelectDto(FederalState selectedFederalState) {
        return federalStateSelectDto(selectedFederalState, false);
    }

    public static HtmlSelectDto federalStateSelectDto(FederalState selectedFederalState, boolean includeGlobalSettingElement) {

        final ArrayList<HtmlOptgroupDto> countries = new ArrayList<>();

        final List<HtmlOptionDto> generalOptions = new ArrayList<>();
        if (includeGlobalSettingElement) {
            final HtmlOptionDto globalOption = new HtmlOptionDto("federalState.GLOBAL", FederalState.GLOBAL.name(), FederalState.GLOBAL.equals(selectedFederalState));
            generalOptions.add(globalOption);
        }

        final HtmlOptionDto noneOption = new HtmlOptionDto("federalState.NONE", FederalState.NONE.name(), FederalState.NONE.equals(selectedFederalState));
        generalOptions.add(noneOption);

        countries.add(new HtmlOptgroupDto("country.general", generalOptions));

        FederalState.federalStatesTypesByCountry().forEach((country, federalStates) -> {
            final List<HtmlOptionDto> options = federalStates.stream()
                .map(federalState -> new HtmlOptionDto(federalStateMessageKey(federalState), federalState.name(), federalState.equals(selectedFederalState)))
                .toList();
            final HtmlOptgroupDto optgroup = new HtmlOptgroupDto("country." + country, options);
            countries.add(optgroup);
        });

        return new HtmlSelectDto(countries);
    }

    public static String federalStateMessageKey(FederalState federalState) {
        return "federalState." + federalState.name();
    }
}
