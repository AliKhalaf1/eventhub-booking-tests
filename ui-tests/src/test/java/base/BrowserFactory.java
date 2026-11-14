package base;

import core.Config;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

final class BrowserFactory {

    private BrowserFactory() {
    }

    static Browser launch(Playwright playwright) {
        String browser = Config.browser();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(Config.headless());
        return switch (browser) {
            case "chrome" -> playwright.chromium().launch(options.setChannel("chrome"));
            case "chromium" -> playwright.chromium().launch(options);
            case "firefox" -> playwright.firefox().launch(options);
            case "webkit" -> playwright.webkit().launch(options);
            default -> throw new IllegalArgumentException(
                    "Unsupported BROWSER '" + browser + "'. Use chrome, chromium, firefox or webkit.");
        };
    }
}
