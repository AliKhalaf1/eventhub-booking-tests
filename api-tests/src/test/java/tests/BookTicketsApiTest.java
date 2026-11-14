package tests;

import api.Api;
import base.BaseApiTest;
import data.BookingDataFactory;
import models.ApiResponse;
import models.Booking;
import models.CreateBookingRequest;
import models.Event;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static data.BookingDataFactory.MAX_TICKETS_PER_BOOKING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("US-114 API: book tickets")
class BookTicketsApiTest extends BaseApiTest {

    @Test
    @DisplayName("A valid booking is confirmed with a reference and total = price x quantity")
    void createsConfirmedBookingWithReferenceAndCorrectTotal() {
        int tickets = 3;
        Event event = testEvents.withAvailableSeats(tickets);
        CreateBookingRequest request = BookingDataFactory.validBooking(event.id(), tickets);

        Response response = api.bookings().create(request);

        assertThat(response.statusCode()).as("POST /bookings status (body: %s)", response.asString()).isEqualTo(201);
        Booking booking = ApiResponse.of(response, Booking.class).data();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(booking.bookingRef()).as("booking reference").isNotBlank();
            softly.assertThat(booking.totalPrice()).as("total = price x quantity")
                    .isEqualByComparingTo(event.price().multiply(BigDecimal.valueOf(tickets)));
            softly.assertThat(booking.quantity()).as("quantity").isEqualTo(tickets);
            softly.assertThat(booking.status()).as("status").isEqualTo("confirmed");
            softly.assertThat(booking.eventId()).as("event id").isEqualTo(event.id());
            softly.assertThat(booking.customerName()).as("customer name").isEqualTo(request.customerName());
            softly.assertThat(booking.customerEmail()).as("customer email").isEqualTo(request.customerEmail());
            softly.assertThat(booking.customerPhone()).as("customer phone").isEqualTo(request.customerPhone());
        });
    }

    @Test
    @DisplayName("The booking reference identifies the booking (lookup by reference)")
    void bookingCanBeRetrievedByItsReference() {
        Event event = testEvents.withAvailableSeats(1);
        Booking created = testBookings.create(event.id(), 1);

        Response response = api.bookings().getByRef(created.bookingRef());

        assertThat(response.statusCode()).as("GET /bookings/ref/%s status", created.bookingRef()).isEqualTo(200);
        Booking found = ApiResponse.of(response, Booking.class).data();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(found.id()).as("id").isEqualTo(created.id());
            softly.assertThat(found.bookingRef()).as("reference").isEqualTo(created.bookingRef());
        });
    }

    @Test
    @DisplayName("A created booking appears in the user's booking list with matching details")
    void createdBookingAppearsInBookingList() {
        Event event = testEvents.withAvailableSeats(2);
        Booking created = testBookings.create(event.id(), 2);

        Response response = api.bookings().list();

        assertThat(response.statusCode()).as("GET /bookings status").isEqualTo(200);
        List<Booking> bookings = ApiResponse.ofList(response, Booking.class).data();
        Booking listed = bookings.stream().filter(b -> created.id().equals(b.id())).findFirst().orElse(null);
        assertThat(listed).as("booking %s in GET /bookings", created.bookingRef()).isNotNull();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(listed.bookingRef()).as("reference").isEqualTo(created.bookingRef());
            softly.assertThat(listed.quantity()).as("quantity").isEqualTo(2);
            softly.assertThat(listed.totalPrice()).as("total").isEqualByComparingTo(created.totalPrice());
            softly.assertThat(listed.status()).as("status").isEqualTo("confirmed");
            softly.assertThat(listed.eventId()).as("event id").isEqualTo(event.id());
        });
    }

    @Test
    @DisplayName("A booking can be cancelled and is then gone")
    void cancelledBookingIsRemoved() {
        Event event = testEvents.withAvailableSeats(1);
        Booking booking = testBookings.create(event.id(), 1);

        Response cancel = api.bookings().cancel(booking.id());

        assertThat(cancel.statusCode()).as("DELETE /bookings/%s status", booking.id()).isEqualTo(200);
        List<Booking> remaining = ApiResponse.ofList(api.bookings().list(), Booking.class).data();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(api.bookings().get(booking.id()).statusCode())
                    .as("GET /bookings/%s after cancel", booking.id()).isEqualTo(404);
            softly.assertThat(remaining).as("booking list after cancel")
                    .noneMatch(b -> booking.id().equals(b.id()));
        });
    }

    @Test
    @DisplayName("Seats: available seats drop by the booked quantity and are restored on cancel")
    void seatsAreReservedOnBookingAndReleasedOnCancel() {
        Event event = testEvents.createPrivateEvent(20, new BigDecimal("150"));
        Booking booking = testBookings.create(event.id(), 3);

        int afterBooking = testEvents.get(event.id()).availableSeats();
        api.bookings().cancel(booking.id());
        int afterCancel = testEvents.get(event.id()).availableSeats();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(afterBooking).as("available seats after booking 3 of 20").isEqualTo(17);
            softly.assertThat(afterCancel).as("available seats after cancelling").isEqualTo(20);
        });
    }

    static Stream<Arguments> invalidContactDetails() {
        return Stream.of(
                arguments("name missing", "customerName", (UnaryOperator<CreateBookingRequest>) r -> r.withCustomerName(null)),
                arguments("email missing", "customerEmail", (UnaryOperator<CreateBookingRequest>) r -> r.withCustomerEmail(null)),
                arguments("phone missing", "customerPhone", (UnaryOperator<CreateBookingRequest>) r -> r.withCustomerPhone(null)),
                arguments("email invalid", "customerEmail", (UnaryOperator<CreateBookingRequest>) r -> r.withCustomerEmail("not-an-email")));
    }

    @ParameterizedTest(name = "{0} -> 400 on {1}")
    @MethodSource("invalidContactDetails")
    @DisplayName("Missing or invalid contact details are rejected with a validation error")
    void rejectsMissingOrInvalidContactDetails(String scenario, String field, UnaryOperator<CreateBookingRequest> breakIt) {
        Event event = testEvents.withAvailableSeats(1);
        CreateBookingRequest request = breakIt.apply(BookingDataFactory.validBooking(event.id(), 1));

        Response response = api.bookings().create(request);

        assertThat(response.statusCode()).as("%s: status (body: %s)", scenario, response.asString()).isEqualTo(400);
        ApiResponse<Void> error = ApiResponse.error(response);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(error.error()).as("error").isEqualTo("Validation failed");
            softly.assertThat(error.invalidFields()).as("rejected fields").containsExactly(field);
        });
    }

    @ParameterizedTest(name = "{0} ticket(s) -> accepted")
    @ValueSource(ints = {1, MAX_TICKETS_PER_BOOKING})
    @DisplayName("Boundary: 1 and 10 tickets are accepted")
    void acceptsTicketCountsWithinLimit(int tickets) {
        Event event = testEvents.withAvailableSeats(MAX_TICKETS_PER_BOOKING);

        Response response = api.bookings().create(BookingDataFactory.validBooking(event.id(), tickets));

        assertThat(response.statusCode()).as("%d tickets: status (body: %s)", tickets, response.asString()).isEqualTo(201);
        Booking booking = ApiResponse.of(response, Booking.class).data();
        assertThat(booking.totalPrice()).as("total = price x %d", tickets)
                .isEqualByComparingTo(event.price().multiply(BigDecimal.valueOf(tickets)));
    }

    static Stream<Arguments> invalidTicketCounts() {
        return Stream.of(
                arguments(0, "zero"),
                arguments(-1, "negative"),
                arguments(MAX_TICKETS_PER_BOOKING + 1, "above the per-booking limit"),
                arguments(1.5, "not a whole number"));
    }

    @ParameterizedTest(name = "{0} tickets ({1}) -> rejected")
    @MethodSource("invalidTicketCounts")
    @DisplayName("Boundary: zero, negative, fractional and more than 10 tickets are rejected")
    void rejectsTicketCountsOutsideLimit(Object tickets, String reason) {
        Event event = testEvents.withAvailableSeats(MAX_TICKETS_PER_BOOKING);

        Response response = api.bookings().create(BookingDataFactory.validBooking(event.id(), 1).withQuantity(tickets));

        assertThat(response.statusCode()).as("%s tickets: status (body: %s)", tickets, response.asString()).isEqualTo(400);
        assertThat(ApiResponse.error(response).invalidFields()).as("rejected fields").containsExactly("quantity");
    }

    @Test
    @DisplayName("Boundary: on an event with fewer than 10 seats left, exactly the remaining seats can be booked")
    void ticketLimitIsTheRemainingSeatsWhenFewerThanTen() {
        int seats = 4;
        Event smallEvent = testEvents.createPrivateEvent(seats, new BigDecimal("75"));

        Response oneTooMany = api.bookings().create(BookingDataFactory.validBooking(smallEvent.id(), seats + 1));
        Response exactlyRemaining = api.bookings().create(BookingDataFactory.validBooking(smallEvent.id(), seats));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(oneTooMany.statusCode()).as("%d of %d seats: status", seats + 1, seats).isEqualTo(400);
            softly.assertThat(ApiResponse.error(oneTooMany).error()).as("error message")
                    .contains("Only " + seats + " seat(s) available");
            softly.assertThat(exactlyRemaining.statusCode()).as("%d of %d seats: status", seats, seats).isEqualTo(201);
        });
    }

    @Test
    @DisplayName("Requests without a token are rejected with 401 on every booking endpoint")
    void bookingEndpointsRequireAuthentication() {
        Event event = testEvents.withAvailableSeats(1);
        Booking booking = testBookings.create(event.id(), 1);
        Api anonymous = Api.anonymous();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(anonymous.bookings().create(BookingDataFactory.validBooking(event.id(), 1)).statusCode())
                    .as("POST /bookings without token").isEqualTo(401);
            softly.assertThat(anonymous.bookings().list().statusCode())
                    .as("GET /bookings without token").isEqualTo(401);
            softly.assertThat(anonymous.bookings().get(booking.id()).statusCode())
                    .as("GET /bookings/{id} without token").isEqualTo(401);
            softly.assertThat(anonymous.bookings().getByRef(booking.bookingRef()).statusCode())
                    .as("GET /bookings/ref/{ref} without token").isEqualTo(401);
            softly.assertThat(anonymous.bookings().cancel(booking.id()).statusCode())
                    .as("DELETE /bookings/{id} without token").isEqualTo(401);
        });
        assertThat(api.bookings().get(booking.id()).statusCode())
                .as("booking still exists after anonymous cancel attempt").isEqualTo(200);
    }

    @Test
    @DisplayName("Another user can neither see nor cancel my booking")
    void otherUserCannotAccessMyBooking() {
        Api otherUser = secondUser();
        Event event = testEvents.withAvailableSeats(1);
        Booking mine = testBookings.create(event.id(), 1);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(otherUser.bookings().get(mine.id()).statusCode())
                    .as("other user: GET /bookings/{id}").isIn(403, 404);
            softly.assertThat(otherUser.bookings().getByRef(mine.bookingRef()).statusCode())
                    .as("other user: GET /bookings/ref/{ref}").isIn(403, 404);
            softly.assertThat(ApiResponse.ofList(otherUser.bookings().list(), Booking.class).data())
                    .as("other user's booking list").noneMatch(b -> mine.id().equals(b.id()));
            softly.assertThat(otherUser.bookings().cancel(mine.id()).statusCode())
                    .as("other user: DELETE /bookings/{id}").isIn(403, 404);
        });
        assertThat(api.bookings().get(mine.id()).statusCode())
                .as("my booking still exists after the other user's cancel attempt").isEqualTo(200);
    }
}
