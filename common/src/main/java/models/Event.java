package models;

import java.math.BigDecimal;

public record Event(
        Integer id,
        String title,
        String description,
        String category,
        String venue,
        String city,
        String eventDate,
        BigDecimal price,
        Integer totalSeats,
        Integer availableSeats,
        String imageUrl,
        Boolean isStatic,
        String createdAt) {
}
