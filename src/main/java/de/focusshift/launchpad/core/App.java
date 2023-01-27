package de.focusshift.launchpad.core;

import java.net.URL;
import java.util.Optional;

record App(URL url, AppName appName, String icon, Optional<String> authority) {
}
