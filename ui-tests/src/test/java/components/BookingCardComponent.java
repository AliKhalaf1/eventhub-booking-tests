package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class BookingCardComponent extends BaseComponent {

    private static final Pattern STATUS = Pattern.compile("^(confirmed|cancelled)$");
    private static final Pattern TICKETS = Pattern.compile("\\d+ tickets?$");
    private static final Pattern MONEY = Pattern.compile("^\\$[\\d,.]+$");

    private final Locator card;

    BookingCardComponent(Page page, Locator card) {
        super(page);
        this.card = card;
    }

    public String eventTitle() {
        return card.locator("h3").innerText().trim();
    }

    public String status() {
        return card.getByText(STATUS).innerText().trim();
    }

    public int tickets() {
        return number(card.getByText(TICKETS).innerText());
    }

    public BigDecimal total() {
        return money(card.getByText(MONEY).innerText());
    }

    void clickCancel() {
        card.getByTestId("cancel-booking-btn").click();
    }
}
