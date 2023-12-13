package de.focusshift.zeiterfassung.web.html;

import java.util.List;

public class HtmlSelectDto {

    private final List<HtmlOptionDto> options;
    private final List<HtmlOptgroupDto> optgroups;

    private HtmlSelectDto(List<HtmlOptionDto> options, List<HtmlOptgroupDto> optgroups) {
        this.options = options;
        this.optgroups = optgroups;
    }

    public static HtmlSelectDto withOptions(List<HtmlOptionDto> options) {
        return new HtmlSelectDto(options, List.of());
    }

    public static HtmlSelectDto withGroups(List<HtmlOptgroupDto> optgroups) {
        return new HtmlSelectDto(List.of(), optgroups);
    }

    public boolean hasGroups() {
        return !optgroups.isEmpty();
    }

    public List<HtmlOptionDto> getOptions() {
        return options;
    }

    public List<HtmlOptgroupDto> getOptgroups() {
        return optgroups;
    }
}
