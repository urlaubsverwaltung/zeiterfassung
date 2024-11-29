package de.focusshift.zeiterfassung.avatar;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class AvatarController {

    private final SvgService svgService;

    AvatarController(final SvgService svgService) {
        this.svgService = svgService;
    }

    @GetMapping(value = "/avatar", produces = "image/svg+xml")
    public ResponseEntity<String> avatar(@RequestParam("name") String name, Locale locale) {

        final Map<String, Object> model = Map.of("initials", getInitials(name));
        final String svg = svgService.createSvg("svg/avatar", locale, model);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(60, TimeUnit.MINUTES))
            .contentType(MediaType.valueOf("image/svg+xml"))
            .body(svg);
    }

    private static String getInitials(String niceName) {

        final String normalizedNiceName = niceName.strip();

        final int idxLastWhitespace = normalizedNiceName.lastIndexOf(' ');
        if (idxLastWhitespace == -1) {
            return normalizedNiceName.substring(0, 1).toUpperCase();
        }

        return (normalizedNiceName.charAt(0) + normalizedNiceName.substring(idxLastWhitespace + 1, idxLastWhitespace + 2)).toUpperCase();
    }
}
