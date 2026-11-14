package components;

import data.Customer;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class BookingFormComponent extends BaseComponent {

    private final Locator form;
    private final Locator ticketCount;
    private final Locator increaseTickets;
    private final Locator decreaseTickets;
    private final Locator maxTicketsHint;
    private final Locator fullName;
    private final Locator email;
    private final Locator phone;
    private final Locator total;
    private final Locator fieldErrors;
    private final Locator confirmButton;

    public BookingFormComponent(Page page) {
        super(page);
        this.form = page.locator("form:has(#confirm-booking)");
        this.ticketCount = form.locator("#ticket-count");
        this.increaseTickets = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("+").setExact(true));
        this.decreaseTickets = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("−").setExact(true));
        this.maxTicketsHint = form.getByText(Pattern.compile("\\(max \\d+\\)"));
        this.fullName = form.getByLabel("Full Name");
        this.email = form.getByTestId("customer-email");
        this.phone = form.getByLabel("Phone Number");
        this.total = form.locator("div:has(> span:text-is('Total')) > span").last();
        this.fieldErrors = form.locator("input + p");
        this.confirmButton = form.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Confirm Booking"));
    }

    public int ticketCount() {
        return number(ticketCount.innerText());
    }

    public BookingFormComponent setTickets(int tickets) {
        while (ticketCount() < tickets) {
            if (!increaseTickets.isEnabled()) {
                throw new IllegalStateException("Ticket selector stops at " + ticketCount() + ", cannot select " + tickets);
            }
            clickAndAwaitCount(increaseTickets, ticketCount() + 1);
        }
        while (ticketCount() > tickets) {
            clickAndAwaitCount(decreaseTickets, ticketCount() - 1);
        }
        return this;
    }

    public BookingFormComponent pressIncrease(int times) {
        for (int i = 0; i < times && increaseTickets.isEnabled(); i++) {
            clickAndAwaitCount(increaseTickets, ticketCount() + 1);
        }
        return this;
    }

    public boolean canIncreaseTickets() {
        return increaseTickets.isEnabled();
    }

    public int maxTicketsShown() {
        return number(maxTicketsHint.innerText());
    }

    public BookingFormComponent fill(Customer customer) {
        fullName.fill(customer.name());
        email.fill(customer.email());
        phone.fill(customer.phone());
        return this;
    }

    public BigDecimal total() {
        return money(total.innerText());
    }

    public void confirm() {
        confirmButton.click();
    }

    public List<String> validationErrors() {
        if (!reaches(fieldErrors.first(), WaitForSelectorState.VISIBLE)) {
            return List.of();
        }
        return fieldErrors.allInnerTexts().stream().map(String::trim).toList();
    }

    private void clickAndAwaitCount(Locator button, int expectedCount) {
        button.click();
        assertThat(ticketCount).hasText(String.valueOf(expectedCount));
    }
}
