package de.focusshift.zeiterfassung;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootWebController {

    @GetMapping("/")
    public String index() {
        return "redirect:/timeentries";
    }
}
