# EventHub: booking flow automation

UI and API tests for the **"Book tickets for an event"** flow of [EventHub](https://eventhub.rahulshettyacademy.com).

**Stack:** Java 17 · Maven · JUnit 5 · Playwright for Java · RestAssured · AssertJ · GitHub Actions

> The story review, findings, test design and decisions are in **[ISSUES.md](ISSUES.md)**.

---

## Quick start

**You need:** Java 17+ and Google Chrome. The Maven Wrapper (`mvnw`) downloads Maven for you.

1. Copy `config.local.properties.example` to `config.local.properties` and fill in the credentials:

   ```properties
   USER_EMAIL=...
   USER_PASSWORD=...
   USER2_EMAIL=...      # optional: second account for the cross-user test
   USER2_PASSWORD=...
   ```

2. Install the Playwright browser (once):

   ```bash
   ./mvnw -pl ui-tests exec:java -Dexec.args="install --with-deps chromium"
   ```

3. Run the tests:

   ```bash
   ./mvnw test -fae                  # everything
   ./mvnw -pl api-tests -am test     # API tests only
   ./mvnw -pl ui-tests -am test      # UI tests only
   ```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Settings

Each setting can be put in `config.local.properties`, set as an environment variable, or passed as `-DKEY=value`.
If a setting appears in more than one place, `-D` wins, then the environment variable, then the file.

| Setting | Default | Purpose |
|---|---|---|
| `USER_EMAIL` / `USER_PASSWORD` | (required) | Main test account |
| `USER2_EMAIL` / `USER2_PASSWORD` | (empty) | Second account. The cross-user test is skipped without it |
| `BASE_URL` | `https://eventhub.rahulshettyacademy.com` | Web app |
| `API_URL` | `https://api.eventhub.rahulshettyacademy.com/api` | API |
| `BROWSER` | `chrome` | `chrome`, `chromium`, `firefox` or `webkit` |
| `HEADLESS` | `true` | Set to `false` to watch the browser |
| `UI_TIMEOUT_MS` | `30000` | Maximum wait for a UI element |
| `RETRY_COUNT` | `1` | Re-runs of a failed UI test. API tests are never retried |

Examples:

```bash
./mvnw -pl ui-tests -am test -DHEADLESS=false
./mvnw -pl ui-tests -am test -DBROWSER=firefox
./mvnw -pl api-tests -am test -Dtest=BookTicketsApiTest
```

## Results

- **Console:** each test prints a progress line, and a summary appears at the end:
  ```
  [3/18] PASSED BookTicketsApiTest.acceptsTicketCountsWithinLimit 1 ticket(s) -> accepted (1.9s)
  Summary: 17 passed, 0 failed, 1 skipped, 0 flaky - 18 test(s) in 71.3s
  ```
- **Reports** are written to `*/target/surefire-reports/`. A UI test that passes only on a retry is reported as **flaky**, not as a plain pass.
- **When a UI test fails**, a screenshot is saved to `ui-tests/target/screenshots/` and a Playwright trace to
  `ui-tests/target/playwright-traces/`. To open a trace, run `npx playwright show-trace <file>`.

## CI

[.github/workflows/tests.yml](.github/workflows/tests.yml) runs the API and UI tests (headless Chromium) on every push and pull request.
It reads the credentials from the repository secrets `USER_EMAIL`, `USER_PASSWORD`, `USER2_EMAIL` and `USER2_PASSWORD`.
Reports are uploaded as build artifacts. Screenshots and traces are uploaded too when a test fails.

## Project layout

```
common/      shared framework code: config, auth, API clients, test data, models
api-tests/   API tests (RestAssured)
ui-tests/    UI tests (Playwright) and UI components
```
