package de.focusshift.launchpad.core;

import java.util.List;

class LaunchpadServiceImpl implements LaunchpadService {

    private final LaunchpadConfigProperties appsProperties;

    LaunchpadServiceImpl(LaunchpadConfigProperties appsProperties) {
        this.appsProperties = appsProperties;
    }

    @Override
    public Launchpad getLaunchpad() {
        return new Launchpad(getApplications());
    }

    private List<App> getApplications() {
        return appsProperties.getApps()
            .stream().map(app -> new App(app.url(), app.messageKey(), app.icon()))
            .toList();
    }
}
