package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class EventCatalogComponent extends BaseComponent {

    private final NavBarComponent navBar;
    private final Locator eventCards;

    public EventCatalogComponent(Page page) {
        super(page);
        this.navBar = new NavBarComponent(page);
        this.eventCards = page.getByTestId("event-card");
    }

    public EventCatalogComponent open() {
        navBar.openEvents();
        return this;
    }

    public EventDetailsComponent openEvent(String title) {
        eventCards.filter(new Locator.FilterOptions().setHasText(title)).getByTestId("book-now-btn").click();
        return new EventDetailsComponent(page);
    }
}
