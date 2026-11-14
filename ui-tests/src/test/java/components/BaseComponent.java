package components;

import core.Config;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.math.BigDecimal;

public abstract class BaseComponent {

    protected static final double EXPECTATION_TIMEOUT_MS = Config.uiTimeoutMillis();

    protected final Page page;

    protected BaseComponent(Page page) {
        this.page = page;
    }

    protected static boolean reaches(Locator locator, WaitForSelectorState state) {
        try {
            locator.waitFor(new Locator.WaitForOptions().setState(state).setTimeout(EXPECTATION_TIMEOUT_MS));
            return true;
        } catch (TimeoutError e) {
            return false;
        }
    }

    protected static BigDecimal money(String text) {
        return new BigDecimal(text.replaceAll("[^0-9.]", ""));
    }

    protected static int number(String text) {
        return Integer.parseInt(text.replaceAll("^\\D*(\\d+).*$", "$1"));
    }
}
