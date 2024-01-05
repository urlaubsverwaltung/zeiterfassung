package de.focusshift.zeiterfassung.report;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
class ReportController {

    @GetMapping("/report")
    public String userReport(RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.mergeAttributes(request.getParameterMap());
        return "forward:/report/week";
    }
}
