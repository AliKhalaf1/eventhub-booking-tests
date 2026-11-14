package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class NavBarComponent extends BaseComponent {

    private final Locator loggedInEmail;

    public NavBarComponent(Page page) {
        super(page);
        this.loggedInEmail = page.getByTestId("user-email-display");
    }

    public void openEvents() {
        page.getByTestId("nav-events").click();
        page.waitForURL("**/events");
    }

    public void openMyBookings() {
        page.getByTestId("nav-bookings").click();
        page.waitForURL("**/bookings");
    }

    public String waitUntilLoggedIn() {
        loggedInEmail.waitFor();
        return loggedInEmail.innerText().trim();
    }
}
