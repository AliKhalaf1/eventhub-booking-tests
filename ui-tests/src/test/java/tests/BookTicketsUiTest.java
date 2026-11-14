package tests;

import data.BookingDataFactory;
import data.Customer;
import models.Booking;
import models.Event;
import base.BaseUiTest;
import components.BookingCardComponent;
import components.BookingConfirmationComponent;
import components.BookingFormComponent;
import components.BookingsListComponent;
import components.EventCatalogComponent;
import components.EventDetailsComponent;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static data.BookingDataFactory.MAX_TICKETS_PER_BOOKING;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("US-114 UI: book tickets")
class BookTicketsUiTest extends BaseUiTest {

    @Test
    @DisplayName("Confirming a booking shows a confirmation with a reference and total = price x tickets")
    void bookingShowsConfirmationWithReferenceAndCorrectTotal() {
        int tickets = 3;
        Event event = testEvents.withAvailableSeats(tickets);
        BigDecimal expectedTotal = event.price().multiply(BigDecimal.valueOf(tickets));
        Customer customer = BookingDataFactory.customer();

        EventDetailsComponent eventPage = new EventCatalogComponent(page).open().openEvent(event.title());
        BookingFormComponent form = eventPage.bookingForm().setTickets(tickets).fill(customer);
        BigDecimal totalBeforeConfirming = form.total();
        BookingConfirmationComponent confirmation = eventPage.confirmBooking();

        assertThat(confirmation.bookingRef()).as("booking reference on confirmation").isNotBlank();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(totalBeforeConfirming).as("form total before confirming").isEqualByComparingTo(expectedTotal);
            softly.assertThat(confirmation.total()).as("confirmed total").isEqualByComparingTo(expectedTotal);
            softly.assertThat(confirmation.tickets()).as("confirmed tickets").isEqualTo(tickets);
            softly.assertThat(confirmation.customerName()).as("confirmed customer").isEqualTo(customer.name());
        });
    }

    @Test
    @DisplayName("A booking made in the UI appears in My Bookings with matching details")
    void bookingAppearsInMyBookings() {
        int tickets = 2;
        Event event = testEvents.withAvailableSeats(tickets);

        BookingConfirmationComponent confirmation = new EventCatalogComponent(page).open()
                .openEvent(event.title())
                .book(tickets, BookingDataFactory.customer());
        String bookingRef = confirmation.bookingRef();
        BookingsListComponent myBookings = confirmation.viewMyBookings();

        assertThat(myBookings.isListed(bookingRef)).as("booking %s listed in My Bookings", bookingRef).isTrue();
        BookingCardComponent card = myBookings.booking(bookingRef);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(card.eventTitle()).as("event").isEqualTo(event.title());
            softly.assertThat(card.tickets()).as("tickets").isEqualTo(tickets);
            softly.assertThat(card.total()).as("total")
                    .isEqualByComparingTo(event.price().multiply(BigDecimal.valueOf(tickets)));
            softly.assertThat(card.status()).as("status").isEqualTo("confirmed");
        });
    }

    @Test
    @DisplayName("Cancelling from My Bookings removes the booking")
    void cancellingFromMyBookingsRemovesTheBooking() {
        Event event = testEvents.withAvailableSeats(1);
        Booking booking = testBookings.create(event.id(), 1);
        BookingsListComponent myBookings = new BookingsListComponent(page).open();
        assertThat(myBookings.isListed(booking.bookingRef())).as("precondition: booking listed").isTrue();

        myBookings.cancel(booking.bookingRef());

        assertThat(myBookings.isRemoved(booking.bookingRef())).as("booking %s removed from My Bookings", booking.bookingRef()).isTrue();
        assertThat(api.bookings().get(booking.id()).statusCode()).as("booking via API after UI cancel").isEqualTo(404);
    }

    @Test
    @DisplayName("Submitting with empty mandatory fields is blocked and shows a validation error per field")
    void emptyMandatoryFieldsBlockSubmission() {
        Event event = testEvents.withAvailableSeats(1);
        EventDetailsComponent eventPage = new EventCatalogComponent(page).open().openEvent(event.title());

        eventPage.bookingForm().confirm();

        assertThat(eventPage.bookingForm().validationErrors()).as("validation messages")
                .containsExactlyInAnyOrder("Name must be at least 2 chars", "Enter a valid email", "Enter a valid 10-digit phone");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(bookingRequestsSentByBrowser()).as("booking requests sent").isZero();
            softly.assertThat(eventPage.confirmation().isShown()).as("confirmation shown").isFalse();
        });
    }

    @Test
    @DisplayName("The ticket selector does not go above 10 tickets per booking")
    void ticketSelectorStopsAtTen() {
        Event event = testEvents.withAvailableSeats(MAX_TICKETS_PER_BOOKING + 5);
        BookingFormComponent form = new EventCatalogComponent(page).open().openEvent(event.title()).bookingForm();

        form.pressIncrease(MAX_TICKETS_PER_BOOKING + 2);

        assertSelectorStopsAt(form, MAX_TICKETS_PER_BOOKING, event.price());
    }

    @Test
    @DisplayName("With fewer than 10 seats left, the ticket selector stops at the remaining seats")
    void ticketSelectorStopsAtRemainingSeats() {
        int seats = 4;
        Event event = testEvents.createPrivateEvent(seats, new BigDecimal("60"));
        BookingFormComponent form = new EventCatalogComponent(page).open().openEvent(event.title()).bookingForm();

        form.pressIncrease(seats + 2);

        assertSelectorStopsAt(form, seats, event.price());
    }

    private static void assertSelectorStopsAt(BookingFormComponent form, int limit, BigDecimal price) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(form.ticketCount()).as("selected tickets").isEqualTo(limit);
            softly.assertThat(form.canIncreaseTickets()).as("'+' enabled at the limit").isFalse();
            softly.assertThat(form.maxTicketsShown()).as("'(max N)' hint").isEqualTo(limit);
            softly.assertThat(form.total()).as("total at the limit")
                    .isEqualByComparingTo(price.multiply(BigDecimal.valueOf(limit)));
        });
    }
}
