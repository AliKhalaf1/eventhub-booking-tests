package api;

import models.CreateBookingRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class BookingClient {

    public static final int MAX_PAGE_SIZE = 100;

    private final RequestSpecification spec;

    public BookingClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response create(CreateBookingRequest request) {
        return given(spec).body(request).post("/bookings");
    }

    public Response list() {
        return given(spec).queryParam("limit", MAX_PAGE_SIZE).get("/bookings");
    }

    public Response get(int bookingId) {
        return given(spec).pathParam("id", bookingId).get("/bookings/{id}");
    }

    public Response getByRef(String bookingRef) {
        return given(spec).pathParam("ref", bookingRef).get("/bookings/ref/{ref}");
    }

    public Response cancel(int bookingId) {
        return given(spec).pathParam("id", bookingId).delete("/bookings/{id}");
    }
}
