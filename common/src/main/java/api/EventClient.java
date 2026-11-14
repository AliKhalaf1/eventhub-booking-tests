package api;

import models.CreateEventRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class EventClient {

    public static final int MAX_PAGE_SIZE = 100;

    private final RequestSpecification spec;

    public EventClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response list() {
        return given(spec).queryParam("limit", MAX_PAGE_SIZE).get("/events");
    }

    public Response get(int eventId) {
        return given(spec).pathParam("id", eventId).get("/events/{id}");
    }

    public Response create(CreateEventRequest request) {
        return given(spec).body(request).post("/events");
    }

    public Response delete(int eventId) {
        return given(spec).pathParam("id", eventId).delete("/events/{id}");
    }
}
