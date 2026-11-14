package api;

import core.TestUser;
import core.CleanupTracker;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleanupTrackingFilter implements Filter {

    private static final Pattern COLLECTION = Pattern.compile(".*/(bookings|events)/?$");
    private static final Pattern ITEM = Pattern.compile(".*/(bookings|events)/(\\d+)/?$");

    private final CleanupTracker tracker;
    private final TestUser owner;

    public CleanupTrackingFilter(CleanupTracker tracker, TestUser owner) {
        this.tracker = tracker;
        this.owner = owner;
    }

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        Response response = ctx.next(request, responseSpec);
        String path = URI.create(request.getURI()).getPath();
        String method = request.getMethod();

        if ("POST".equals(method) && response.statusCode() == 201) {
            Matcher m = COLLECTION.matcher(path);
            Integer id = m.matches() ? response.path("data.id") : null;
            if (id != null) {
                if ("bookings".equals(m.group(1))) {
                    tracker.registerBooking(id, owner);
                } else {
                    tracker.registerEvent(id, owner);
                }
            }
        } else if ("DELETE".equals(method) && response.statusCode() == 200) {
            Matcher m = ITEM.matcher(path);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(2));
                if ("bookings".equals(m.group(1))) {
                    tracker.forgetBooking(id);
                } else {
                    tracker.forgetEvent(id);
                }
            }
        }
        return response;
    }
}
