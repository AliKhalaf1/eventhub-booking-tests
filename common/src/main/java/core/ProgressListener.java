package core;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressListener implements TestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger("Progress");

    private static final Set<String> FAILED_BEFORE = ConcurrentHashMap.newKeySet();

    private final Map<String, Long> startNanos = new ConcurrentHashMap<>();
    private final AtomicInteger planned = new AtomicInteger();
    private final AtomicInteger done = new AtomicInteger();
    private final AtomicInteger passed = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger flaky = new AtomicInteger();
    private volatile TestPlan plan;
    private volatile long planStartNanos;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        plan = testPlan;
        planStartNanos = System.nanoTime();
        for (AtomicInteger counter : new AtomicInteger[]{done, passed, failed, skipped, flaky}) {
            counter.set(0);
        }
        planned.set((int) testPlan.countTestIdentifiers(TestIdentifier::isTest));
        long parameterized = testPlan.countTestIdentifiers(id -> id.getUniqueId().contains("[test-template:"));
        if (FAILED_BEFORE.isEmpty()) {
            log.info("Starting test run: {} test(s) planned{}", planned.get(), parameterized == 0 ? ""
                    : " + " + parameterized + " parameterized test(s) whose cases are counted as they start");
        } else {
            log.info("Re-running {} failed test(s)", planned.get());
        }
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier id) {
        if (id.isTest()) {
            planned.incrementAndGet();
        }
    }

    @Override
    public void executionStarted(TestIdentifier id) {
        if (id.isTest()) {
            startNanos.put(id.getUniqueId(), System.nanoTime());
        }
    }

    @Override
    public void executionSkipped(TestIdentifier id, String reason) {
        if (id.isTest()) {
            skipped.incrementAndGet();
            log.info("[{}/{}] SKIPPED {} - {}", done.incrementAndGet(), planned.get(), name(id), reason);
        } else if (plan != null) {
            plan.getDescendants(id).stream().filter(TestIdentifier::isTest).forEach(test -> {
                skipped.incrementAndGet();
                log.info("[{}/{}] SKIPPED {} - {}", done.incrementAndGet(), planned.get(), name(test), reason);
            });
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (!id.isTest()) {
            return;
        }
        String key = id.getUniqueId();
        Long start = startNanos.remove(key);
        String duration = start == null ? "" : String.format(" (%.1fs)", (System.nanoTime() - start) / 1e9);
        String progress = "[" + done.incrementAndGet() + "/" + planned.get() + "]";
        String reason = result.getThrowable().map(t -> " - " + firstLine(t)).orElse("");

        switch (result.getStatus()) {
            case SUCCESSFUL -> {
                if (FAILED_BEFORE.remove(key)) {
                    flaky.incrementAndGet();
                    log.warn("{} PASSED (FLAKY - passed on re-run) {}{}", progress, name(id), duration);
                } else {
                    passed.incrementAndGet();
                    log.info("{} PASSED {}{}", progress, name(id), duration);
                }
            }
            case ABORTED -> {
                skipped.incrementAndGet();
                log.info("{} SKIPPED {}{}{}", progress, name(id), duration, reason);
            }
            case FAILED -> {
                failed.incrementAndGet();
                FAILED_BEFORE.add(key);
                log.error("{} FAILED {}{}{}", progress, name(id), duration, reason);
            }
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        double seconds = (System.nanoTime() - planStartNanos) / 1e9;
        log.info("Summary: {} passed, {} failed, {} skipped, {} flaky - {} test(s) in {}s",
                passed.get(), failed.get(), skipped.get(), flaky.get(), done.get(), String.format("%.1f", seconds));
    }

    private static String name(TestIdentifier id) {
        String base = id.getSource()
                .filter(MethodSource.class::isInstance)
                .map(MethodSource.class::cast)
                .map(source -> simpleName(source.getClassName()) + "." + source.getMethodName())
                .orElse(id.getDisplayName());
        return id.getUniqueId().contains("-invocation:") ? base + " " + id.getDisplayName() : base;
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1).replace('$', '.');
    }

    private static String firstLine(Throwable t) {
        String message = String.valueOf(t.getMessage()).strip();
        int newline = message.indexOf('\n');
        return t.getClass().getSimpleName() + ": " + (newline < 0 ? message : message.substring(0, newline) + " ...");
    }
}
