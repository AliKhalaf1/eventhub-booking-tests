package base;

import api.Api;
import core.TestUser;
import core.CleanupTracker;
import core.Config;
import data.TestBookings;
import data.TestEvents;
import models.Json;
import components.NavBarComponent;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.WaitUntilState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public abstract class BaseUiTest {

    private static final Logger log = LoggerFactory.getLogger(BaseUiTest.class);
    private static final Pattern CREATE_BOOKING_URL = Pattern.compile(".*/api/bookings/?$");
    private static final Path ARTIFACTS = Paths.get("target");

    private static Playwright playwright;
    private static Browser browser;

    protected final CleanupTracker createdData = new CleanupTracker();
    protected final Api api = Api.as(TestUser.MAIN, createdData);
    protected final TestEvents testEvents = new TestEvents(api.events());
    protected final TestBookings testBookings = new TestBookings(api.bookings());

    protected Page page;
    private BrowserContext context;
    private final AtomicInteger bookingRequestsFromBrowser = new AtomicInteger();
    private boolean testFailed;

    @RegisterExtension
    final AfterTestExecutionCallback failureWatcher = ctx -> testFailed = ctx.getExecutionException().isPresent();

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = BrowserFactory.launch(playwright);
    }

    @BeforeEach
    void openAppAsLoggedInUser() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Config.baseUrl())
                .setViewportSize(1440, 900)
                .setStorageState(BrowserAuth.storageStateFor(TestUser.MAIN)));
        context.setDefaultTimeout(Config.uiTimeoutMillis());
        context.setDefaultNavigationTimeout(Config.uiTimeoutMillis() * 2);
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
        page.onRequest(this::countBookingRequest);
        page.onResponse(this::trackBookingCreatedInBrowser);
        page.navigate("/", new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        String shownUser = new NavBarComponent(page).waitUntilLoggedIn();
        if (!shownUser.equalsIgnoreCase(TestUser.MAIN.email())) {
            throw new IllegalStateException("Browser should be logged in as " + TestUser.MAIN.email() + " but shows " + shownUser);
        }
    }

    @AfterEach
    void closeContextAndCleanUp(TestInfo testInfo) {
        try {
            if (testFailed) {
                String name = artifactName(testInfo);
                page.screenshot(new Page.ScreenshotOptions().setPath(ARTIFACTS.resolve("screenshots/" + name + ".png")).setFullPage(true));
                Path trace = ARTIFACTS.resolve("playwright-traces/" + name + ".zip");
                context.tracing().stop(new Tracing.StopOptions().setPath(trace));
                log.info("Failure evidence saved: {} (open with: npx playwright show-trace)", trace.toAbsolutePath());
            } else {
                context.tracing().stop();
            }
            context.close();
        } finally {
            createdData.cleanUp();
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    protected int bookingRequestsSentByBrowser() {
        return bookingRequestsFromBrowser.get();
    }

    private void countBookingRequest(Request request) {
        if (isCreateBooking(request)) {
            bookingRequestsFromBrowser.incrementAndGet();
        }
    }

    private void trackBookingCreatedInBrowser(Response response) {
        if (!isCreateBooking(response.request()) || response.status() != 201) {
            return;
        }
        try {
            int id = Json.MAPPER.readTree(response.text()).path("data").path("id").asInt();
            createdData.registerBooking(id, TestUser.MAIN);
        } catch (Exception e) {
            log.warn("Could not register booking created in the browser for clean-up", e);
        }
    }

    private static boolean isCreateBooking(Request request) {
        return "POST".equals(request.method()) && CREATE_BOOKING_URL.matcher(request.url()).matches();
    }

    private static String artifactName(TestInfo info) {
        String raw = info.getTestClass().map(Class::getSimpleName).orElse("UiTest") + "."
                + info.getTestMethod().map(m -> m.getName()).orElse("test") + "-" + info.getDisplayName();
        return raw.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
