package data;

import models.CreateBookingRequest;
import net.datafaker.Faker;

import java.util.Locale;
import java.util.UUID;

public final class BookingDataFactory {

    public static final int MAX_TICKETS_PER_BOOKING = 10;

    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(() -> new Faker(Locale.ENGLISH));

    private BookingDataFactory() {
    }

    public static Customer customer() {
        Faker faker = FAKER.get();
        String first = faker.name().firstName();
        String last = faker.name().lastName();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = ("qa." + first + "." + last + "." + unique + "@example.com")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.@]", "");
        String phone = faker.numerify("+1 (###) ###-####");
        return new Customer(first + " " + last, email, phone);
    }

    public static CreateBookingRequest validBooking(int eventId, int quantity) {
        Customer customer = customer();
        return new CreateBookingRequest(eventId, customer.name(), customer.email(), customer.phone(), quantity);
    }
}
