package models;

import java.math.BigDecimal;

public record Booking(
        Integer id,
        Integer eventId,
        Integer userId,
        String customerName,
        String customerEmail,
        String customerPhone,
        Integer quantity,
        BigDecimal totalPrice,
        String status,
        String bookingRef,
        String createdAt,
        Event event) {
}
