package de.focusshift.zeiterfassung.web.html;

import java.util.List;

public record HtmlOptgroupDto(String labelMessageKey, List<HtmlOptionDto> options) {
}
