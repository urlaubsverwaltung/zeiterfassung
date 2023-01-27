package de.focusshift.launchpad.core;

import de.focusshift.launchpad.api.LaunchpadAppUrlCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

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
            .map(this::toApp)
            .filter(Optional::isPresent)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<App> toApp(LaunchpadConfigProperties.App app){
        return getAppUrl(app)
            .map(url -> new App(url, new AppName(app.name().get(appsProperties.getNameDefaultLocale()), app.name()), app.icon()));
    }

    private Optional<URL> getAppUrl(LaunchpadConfigProperties.App app) {
        try {
            return Optional.of(appUrlCustomizer.customize(app.url()));
        } catch (MalformedURLException e) {
            LOG.info("ignoring launchpad app: could not build URL for app={}", app, e);
            return Optional.empty();
        }
    }
}
