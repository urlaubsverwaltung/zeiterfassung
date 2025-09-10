package de.focusshift.zeiterfassung.ui.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;

import static com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED;

/**
 * The LoginPage class represents a login page in a web application.
 * It provides methods to fill the login form with credentials and submit the form.
 */
public class LoginPage {

    private static final String USERNAME_SELECTOR = "#username";
    private static final String PASSWORD_SELECTOR = "#password";
    private static final String SUBMIT_SELECTOR = "#kc-login";

    private final Page page;
    private final int port;

    public LoginPage(Page page, int port) {
        this.page = page;
        this.port = port;
    }

    /**
     * Fills the login form with the given credentials and submits the login form.
     *
     * @param credentials username and password
     */
    public void login(Credentials credentials) {
        page.navigate("http://localhost:" + port + "/oauth2/authorization/keycloak");
        page.fill(USERNAME_SELECTOR, credentials.username);
        page.fill(PASSWORD_SELECTOR, credentials.password);
        page.waitForResponse(Response::ok, () -> page.locator(SUBMIT_SELECTOR).click());
        page.waitForLoadState(DOMCONTENTLOADED);
    }

    public record Credentials(String username, String password) {
    }
}
