package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public class BookingsListComponent extends BaseComponent {

    private final NavBarComponent navBar;
    private final ConfirmDialogComponent confirmDialog;
    private final Locator bookingCards;

    public BookingsListComponent(Page page) {
        super(page);
        this.navBar = new NavBarComponent(page);
        this.confirmDialog = new ConfirmDialogComponent(page);
        this.bookingCards = page.getByTestId("booking-card");
    }

    public BookingsListComponent open() {
        navBar.openMyBookings();
        return this;
    }

    public boolean isListed(String bookingRef) {
        return reaches(card(bookingRef), WaitForSelectorState.VISIBLE);
    }

    public boolean isRemoved(String bookingRef) {
        return reaches(card(bookingRef), WaitForSelectorState.DETACHED);
    }

    public BookingCardComponent booking(String bookingRef) {
        return new BookingCardComponent(page, card(bookingRef));
    }

    public void cancel(String bookingRef) {
        booking(bookingRef).clickCancel();
        confirmDialog.confirm();
    }

    private Locator card(String bookingRef) {
        return bookingCards.filter(new Locator.FilterOptions().setHasText(bookingRef));
    }
}
