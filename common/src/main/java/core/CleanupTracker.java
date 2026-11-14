package core;

import api.Api;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class CleanupTracker {

    private static final Logger log = LoggerFactory.getLogger(CleanupTracker.class);

    private final Map<Integer, TestUser> bookings = new LinkedHashMap<>();
    private final Map<Integer, TestUser> events = new LinkedHashMap<>();

    public synchronized void registerBooking(int bookingId, TestUser owner) {
        bookings.put(bookingId, owner);
    }

    public synchronized void forgetBooking(int bookingId) {
        bookings.remove(bookingId);
    }

    public synchronized void registerEvent(int eventId, TestUser owner) {
        events.put(eventId, owner);
    }

    public synchronized void forgetEvent(int eventId) {
        events.remove(eventId);
    }

    public synchronized void cleanUp() {
        remove("booking", bookings, (api, id) -> api.bookings().cancel(id));
        remove("event", events, (api, id) -> api.events().delete(id));
    }

    private static void remove(String kind, Map<Integer, TestUser> items,
                               BiFunction<Api, Integer, Response> deleteCall) {
        items.forEach((id, owner) -> {
            try {
                Response response = deleteCall.apply(Api.as(owner, null), id);
                if (response.statusCode() == 200 || response.statusCode() == 404) {
                    log.debug("Clean-up: {} {} removed (HTTP {})", kind, id, response.statusCode());
                } else {
                    log.warn("Clean-up: could not remove {} {} as {}: HTTP {} {}",
                            kind, id, owner, response.statusCode(), response.asString());
                }
            } catch (RuntimeException e) {
                log.warn("Clean-up: could not remove {} {} as {}", kind, id, owner, e);
            }
        });
        items.clear();
    }
}
