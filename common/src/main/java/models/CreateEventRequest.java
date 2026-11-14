package models;

import java.math.BigDecimal;

public record CreateEventRequest(
        String title,
        String description,
        String category,
        String venue,
        String city,
        String eventDate,
        BigDecimal price,
        Integer totalSeats) {
}
