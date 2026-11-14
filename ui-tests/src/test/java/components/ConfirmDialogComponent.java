package components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

public class ConfirmDialogComponent extends BaseComponent {

    private final Locator dialog;

    public ConfirmDialogComponent(Page page) {
        super(page);
        this.dialog = page.getByRole(AriaRole.DIALOG);
    }

    public void confirm() {
        dialog.getByTestId("confirm-dialog-yes").click();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }
}
