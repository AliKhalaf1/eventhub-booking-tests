package models;

import com.fasterxml.jackson.databind.JavaType;
import io.restassured.response.Response;

import java.util.List;

public record ApiResponse<T>(
        T data,
        String error,
        List<FieldError> details) {

    public static <T> ApiResponse<T> of(Response response, Class<T> dataType) {
        JavaType type = Json.MAPPER.getTypeFactory().constructParametricType(ApiResponse.class, dataType);
        return Json.read(response.asString(), type);
    }

    public static <T> ApiResponse<List<T>> ofList(Response response, Class<T> itemType) {
        JavaType listType = Json.MAPPER.getTypeFactory().constructCollectionType(List.class, itemType);
        JavaType type = Json.MAPPER.getTypeFactory().constructParametricType(ApiResponse.class, listType);
        return Json.read(response.asString(), type);
    }

    public static ApiResponse<Void> error(Response response) {
        return of(response, Void.class);
    }

    public List<String> invalidFields() {
        return details == null ? List.of() : details.stream().map(FieldError::field).distinct().toList();
    }

    public record FieldError(String field, String message) {
    }
}
