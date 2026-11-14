package components;

import data.Customer;
import com.microsoft.playwright.Page;

public class EventDetailsComponent extends BaseComponent {

    private final BookingFormComponent bookingForm;
    private final BookingConfirmationComponent confirmation;

    public EventDetailsComponent(Page page) {
        super(page);
        this.bookingForm = new BookingFormComponent(page);
        this.confirmation = new BookingConfirmationComponent(page);
    }

    public BookingFormComponent bookingForm() {
        return bookingForm;
    }

    public BookingConfirmationComponent confirmation() {
        return confirmation;
    }

    public BookingConfirmationComponent confirmBooking() {
        bookingForm.confirm();
        return confirmation.waitUntilShown();
    }

    public BookingConfirmationComponent book(int tickets, Customer customer) {
        bookingForm.setTickets(tickets);
        bookingForm.fill(customer);
        return confirmBooking();
    }
}
