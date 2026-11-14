package api;

import models.LoginRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class AuthClient {

    private final RequestSpecification spec;

    public AuthClient(RequestSpecification spec) {
        this.spec = spec;
    }

    public Response login(LoginRequest request) {
        return given(spec).body(request).post("/auth/login");
    }
}
