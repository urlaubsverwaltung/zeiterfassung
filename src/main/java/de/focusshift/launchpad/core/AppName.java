package de.focusshift.launchpad.core;

import java.util.Locale;
import java.util.Map;

record AppName(String defaultName, Map<Locale, String> names) {

    public String get(Locale locale) {
        return names.getOrDefault(locale, defaultName);
    }
}
