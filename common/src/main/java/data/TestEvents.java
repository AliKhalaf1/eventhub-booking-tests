package data;

import api.EventClient;
import models.ApiResponse;
import models.CreateEventRequest;
import models.Event;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class TestEvents {

    private final EventClient events;

    public TestEvents(EventClient events) {
        this.events = events;
    }

    public List<Event> all() {
        Response response = events.list();
        requireStatus(response, 200, "list events");
        return ApiResponse.ofList(response, Event.class).data();
    }

    public Event get(int eventId) {
        Response response = events.get(eventId);
        requireStatus(response, 200, "read event " + eventId);
        return ApiResponse.of(response, Event.class).data();
    }

    public Event withAvailableSeats(int minSeats) {
        return all().stream()
                .filter(e -> e.availableSeats() != null && e.availableSeats() >= minSeats)
                .max(Comparator.comparingInt(Event::availableSeats))
                .orElseThrow(() -> new IllegalStateException(
                        "No event has at least " + minSeats + " available seats; the environment needs test events."));
    }

    public Event createPrivateEvent(int seats, BigDecimal price) {
        CreateEventRequest request = new CreateEventRequest(
                "QA private event " + UUID.randomUUID().toString().substring(0, 8),
                "Temporary event created by automated tests; deleted after the test.",
                "Workshop",
                "QA Test Venue",
                "QA City",
                LocalDate.now(ZoneOffset.UTC).plusMonths(6).atStartOfDay().toInstant(ZoneOffset.UTC).toString(),
                price,
                seats);
        Response response = events.create(request);
        requireStatus(response, 201, "create private test event");
        return ApiResponse.of(response, Event.class).data();
    }

    private static void requireStatus(Response response, int expected, String action) {
        if (response.statusCode() != expected) {
            throw new IllegalStateException("Precondition failed - could not " + action + ": HTTP "
                    + response.statusCode() + " " + response.asString());
        }
    }
}
