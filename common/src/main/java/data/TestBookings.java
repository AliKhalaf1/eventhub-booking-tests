package data;

import api.BookingClient;
import models.ApiResponse;
import models.Booking;
import models.CreateBookingRequest;
import io.restassured.response.Response;

public final class TestBookings {

    private final BookingClient bookings;

    public TestBookings(BookingClient bookings) {
        this.bookings = bookings;
    }

    public Booking create(CreateBookingRequest request) {
        Response response = bookings.create(request);
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Precondition failed - could not create booking " + request
                    + ": HTTP " + response.statusCode() + " " + response.asString());
        }
        return ApiResponse.of(response, Booking.class).data();
    }

    public Booking create(int eventId, int quantity) {
        return create(BookingDataFactory.validBooking(eventId, quantity));
    }
}
