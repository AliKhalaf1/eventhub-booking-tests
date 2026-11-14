package api;

import core.TestUser;
import core.TokenProvider;
import core.CleanupTracker;
import core.Config;
import models.Json;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

public final class Api {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private static final RestAssuredConfig REST_ASSURED_CONFIG = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", CONNECT_TIMEOUT_MS)
                    .setParam("http.socket.timeout", READ_TIMEOUT_MS)
                    .setParam("http.connection-manager.timeout", (long) CONNECT_TIMEOUT_MS))
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((type, charset) -> Json.MAPPER));

    private final RequestSpecification spec;

    private Api(RequestSpecification spec) {
        this.spec = spec;
    }

    public static Api as(TestUser user, CleanupTracker tracker) {
        return new Api(authenticatedSpec(user, tracker));
    }

    public static Api anonymous() {
        return new Api(anonymousSpec());
    }

    public BookingClient bookings() {
        return new BookingClient(spec);
    }

    public EventClient events() {
        return new EventClient(spec);
    }

    public AuthClient auth() {
        return new AuthClient(spec);
    }

    private static RequestSpecification anonymousSpec() {
        return new RequestSpecBuilder()
                .setConfig(REST_ASSURED_CONFIG)
                .setBaseUri(Config.apiUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    private static RequestSpecification authenticatedSpec(TestUser user, CleanupTracker tracker) {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .addRequestSpecification(anonymousSpec())
                .addHeader("Authorization", "Bearer " + TokenProvider.tokenFor(user));
        if (tracker != null) {
            builder.addFilter(new CleanupTrackingFilter(tracker, user));
        }
        return builder.build();
    }
}
