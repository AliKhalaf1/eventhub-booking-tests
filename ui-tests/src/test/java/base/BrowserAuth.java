package base;

import core.TestUser;
import core.TokenProvider;
import core.Config;
import models.Json;

import java.util.List;
import java.util.Map;

final class BrowserAuth {

    static final String TOKEN_STORAGE_KEY = "eventhub_token";

    private BrowserAuth() {
    }

    static String storageStateFor(TestUser user) {
        Map<String, Object> origin = Map.of(
                "origin", Config.baseUrl(),
                "localStorage", List.of(Map.of("name", TOKEN_STORAGE_KEY, "value", TokenProvider.tokenFor(user))));
        return Json.write(Map.of("cookies", List.of(), "origins", List.of(origin)));
    }
}
