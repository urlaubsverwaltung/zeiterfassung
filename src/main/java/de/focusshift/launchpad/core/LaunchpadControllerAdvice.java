package de.focusshift.launchpad.core;

import de.focusshift.launchpad.api.HasLaunchpad;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(assignableTypes = { HasLaunchpad.class })
public class LaunchpadControllerAdvice {

    private final LaunchpadService launchpadService;

    LaunchpadControllerAdvice(LaunchpadService launchpadService) {
        this.launchpadService = launchpadService;
    }

    @ModelAttribute
    public void addAttributes(Model model) {

        final Launchpad launchpad = launchpadService.getLaunchpad();

        final List<AppDto> appDtos = launchpad.apps()
            .stream()
            .map(app -> new AppDto(app.url().toString(), app.messageKey(), app.icon()))
            .toList();

        if (!appDtos.isEmpty()) {
            model.addAttribute("launchpad", new LaunchpadDto(appDtos));
        }
    }
}
