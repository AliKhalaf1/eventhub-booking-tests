package models;

public record CreateBookingRequest(
        Integer eventId,
        String customerName,
        String customerEmail,
        String customerPhone,
        Object quantity) {

    public CreateBookingRequest withCustomerName(String value) {
        return new CreateBookingRequest(eventId, value, customerEmail, customerPhone, quantity);
    }

    public CreateBookingRequest withCustomerEmail(String value) {
        return new CreateBookingRequest(eventId, customerName, value, customerPhone, quantity);
    }

    public CreateBookingRequest withCustomerPhone(String value) {
        return new CreateBookingRequest(eventId, customerName, customerEmail, value, quantity);
    }

    public CreateBookingRequest withQuantity(Object value) {
        return new CreateBookingRequest(eventId, customerName, customerEmail, customerPhone, value);
    }
}
