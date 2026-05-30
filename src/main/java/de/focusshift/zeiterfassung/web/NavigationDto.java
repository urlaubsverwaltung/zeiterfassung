package de.focusshift.zeiterfassung.web;

import java.util.List;
import java.util.stream.Stream;

record NavigationDto(
    List<NavigationItemDto> favorites,
    List<NavigationItemDto> basic,
    List<NavigationItemDto> company,
    List<NavigationItemDto> settings
) {

    /**
     * @return all navigation items across every group, in group order.
     */
    List<NavigationItemDto> items() {
        return Stream.of(favorites, basic, company, settings)
            .flatMap(List::stream)
            .toList();
    }
}
