package de.focusshift.launchpad.core;

import de.focusshift.launchpad.api.LaunchpadAppUrlCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;

class LaunchpadServiceImpl implements LaunchpadService {

    private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());


    private final LaunchpadConfigProperties appsProperties;
    private final LaunchpadAppUrlCustomizer appUrlCustomizer;

    LaunchpadServiceImpl(LaunchpadConfigProperties appsProperties, LaunchpadAppUrlCustomizer appUrlCustomizer) {
        this.appsProperties = appsProperties;
        this.appUrlCustomizer = appUrlCustomizer;
    }

    @Override
    public Launchpad getLaunchpad() {
        return new Launchpad(getApplications());
    }

    private List<App> getApplications() {
        return appsProperties.getApps()
            .stream()
            .map(app -> {
                final URL url;

                try {
                    url = appUrlCustomizer.customize(app.url());
                } catch (MalformedURLException e) {
                    LOG.info("ignoring app because: could not build URL for app={}", app);
                    return null;
                }

                final AppName appName = new AppName(app.name().get(appsProperties.getNameDefaultLocale()), app.name());

                return new App(url, appName, app.icon());
            })
            .filter(Objects::nonNull)
            .toList();
    }
}
