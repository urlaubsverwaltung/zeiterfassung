package de.focusshift.zeiterfassung;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.test.context.DynamicPropertyRegistry;

public class TestKeycloakContainer extends KeycloakContainer {

    private static final String VERSION = "26.5.2";
    private static final String IMAGE = "quay.io/keycloak/keycloak";
    public static final String REALM_ZEITERFASSUNG = "zeiterfassung-realm";

    public TestKeycloakContainer() {
        super(IMAGE + ":" + VERSION);
        this.withRealmImportFiles(
            "/docker/keycloak/export/zeiterfassung-realm-realm.json",
            "/docker/keycloak/export/zeiterfassung-realm-users-0.json"
        );
    }

    /**
     * Sets the spring security client configuration properties.
     *
     * <p>Usage:</p>
     * <pre><code>
     * static final TestKeycloakContainer keycloak = new TestKeycloakContainer();
     * &#64;DynamicPropertySource
     * static void setupDataSource(DynamicPropertySource registry) {
     *     keycloak.start();
     *     keycloak.configureSpringDataSource(registry);
     * }
     * </code>
     * </pre>
     *
     * @param registry {@link DynamicPropertyRegistry} to configure configurations dynamically
     */
    public final void configureSpringDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.registration.keycloak.client-id", () -> "zeiterfassung");
        registry.add("spring.security.oauth2.client.registration.keycloak.client-secret", () -> "zeiterfassung-secret");
        registry.add("spring.security.oauth2.client.registration.keycloak.provider", () -> "keycloak");
        registry.add("spring.security.oauth2.client.registration.keycloak.scope", () -> "openid,profile,email,roles");
        registry.add("spring.security.oauth2.client.registration.keycloak.authorization-grant-type", () -> "authorization_code");
        registry.add("spring.security.oauth2.client.registration.keycloak.redirect-uri", () -> "http://{baseHost}{basePort}/login/oauth2/code/{registrationId}");
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", () -> this.getAuthServerUrl() + "/realms/"+ REALM_ZEITERFASSUNG);
    }
}
