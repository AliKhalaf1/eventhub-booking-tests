package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.math.BigDecimal;

public class BookingConfirmationComponent extends BaseComponent {

    private final Locator panel;

    public BookingConfirmationComponent(Page page) {
        super(page);
        this.panel = page.locator("div:has(> h3:has-text('Booking Confirmed'))");
    }

    public BookingConfirmationComponent waitUntilShown() {
        panel.waitFor();
        return this;
    }

    public boolean isShown() {
        return panel.isVisible();
    }

    public String bookingRef() {
        return valueOf("Booking Ref");
    }

    public String customerName() {
        return valueOf("Customer");
    }

    public int tickets() {
        return number(valueOf("Tickets"));
    }

    public BigDecimal total() {
        return money(valueOf("Total"));
    }

    public BookingsListComponent viewMyBookings() {
        panel.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("View My Bookings")).click();
        return new BookingsListComponent(page);
    }

    private String valueOf(String label) {
        return panel.locator("div:has(> span:text-is('" + label + "')) > span").nth(1).innerText().trim();
    }
}
