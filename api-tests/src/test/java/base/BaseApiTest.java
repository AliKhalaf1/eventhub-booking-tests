package base;

import api.Api;
import core.TestUser;
import core.CleanupTracker;
import data.TestBookings;
import data.TestEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;

public abstract class BaseApiTest {

    protected final CleanupTracker createdData = new CleanupTracker();
    protected final Api api = Api.as(TestUser.MAIN, createdData);
    protected final TestEvents testEvents = new TestEvents(api.events());
    protected final TestBookings testBookings = new TestBookings(api.bookings());

    protected Api secondUser() {
        Assumptions.assumeTrue(TestUser.SECOND.isConfigured(),
                "Second test user not configured (" + TestUser.SECOND.configKeys() + ") - skipping cross-user test");
        return Api.as(TestUser.SECOND, createdData);
    }

    @AfterEach
    void cleanUp() {
        createdData.cleanUp();
    }
}
