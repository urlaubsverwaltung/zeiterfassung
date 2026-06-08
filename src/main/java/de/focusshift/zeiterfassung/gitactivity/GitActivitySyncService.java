package de.focusshift.zeiterfassung.gitactivity;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Scheduler that drives all registered {@link GitActivityProvider} implementations.
 * Each provider is responsible for fetching activity from its own platform (GitHub, GitLab, …).
 */
@Service
class GitActivitySyncService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final List<GitActivityProvider> providers;

    GitActivitySyncService(List<GitActivityProvider> providers) {
        this.providers = providers;
    }

    @Scheduled(fixedDelay = 600_000)
    @SchedulerLock(name = "githubActivitySync", lockAtMostFor = "PT9M", lockAtLeastFor = "PT30S")
    void sync() {
        for (GitActivityProvider provider : providers) {
            if (!provider.isConfigured()) {
                LOG.debug("{} provider not configured ({}), skipping sync",
                    provider.platform(), provider.missingConfig());
                continue;
            }

            final List<String> usernames = provider.resolveUsernames();
            if (usernames.isEmpty()) continue;

            for (String username : usernames) {
                try {
                    provider.syncUser(username);
                } catch (Exception e) {
                    LOG.error("Failed to sync {} activity for user {}", provider.platform(), username, e);
                }
            }
        }
    }
}
