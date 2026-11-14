package core;

import api.Api;
import models.LoginRequest;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);
    private static final Map<TestUser, String> TOKENS = new ConcurrentHashMap<>();

    private TokenProvider() {
    }

    public static String tokenFor(TestUser user) {
        return TOKENS.computeIfAbsent(user, TokenProvider::login);
    }

    private static String login(TestUser user) {
        String email = user.email();
        Response response = Api.anonymous().auth().login(new LoginRequest(email, user.password()));
        String token = response.statusCode() == 200 ? response.jsonPath().getString("token") : null;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(String.format(
                    "Login failed for test user %s (%s): HTTP %d %s. Check %s.",
                    user, email, response.statusCode(), response.asString(), user.configKeys()));
        }
        log.info("Logged in as {} ({}); token cached for this run", user, email);
        return token;
    }
}
