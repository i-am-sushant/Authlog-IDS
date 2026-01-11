package ids.detect;

import ids.alert.AlertService;
import ids.config.DetectionConfig;
import ids.model.EventType;
import ids.model.LoginEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DetectionEngine {
    private final AlertService alertService;

    private final Map<String, Deque<Instant>> ipFailures = new HashMap<>();
    private final Map<String, Deque<Instant>> userFailures = new HashMap<>();
    private final Map<String, Deque<Instant>> failSuccessByUser = new HashMap<>();

    public DetectionEngine(AlertService alertService) {
        this.alertService = Objects.requireNonNull(alertService, "alertService");
    }

    public void process(LoginEvent event) {
        if (event.getEventType() == EventType.FAIL) {
            handleFailure(event);
        } else {
            handleSuccess(event);
        }
    }

    private void handleFailure(LoginEvent event) {
        Instant ts = event.getTimestamp();

        Deque<Instant> ipDeque = ipFailures.computeIfAbsent(event.getIpAddress(), k -> new ArrayDeque<>());
        addAndTrim(ipDeque, ts, DetectionConfig.IP_WINDOW);
        if (ipDeque.size() == DetectionConfig.IP_FAIL_THRESHOLD) {
            alertService.alertIpBruteForce(event.getIpAddress(), ipDeque.size(), DetectionConfig.IP_WINDOW_LABEL, ts);
        }

        Deque<Instant> userDeque = userFailures.computeIfAbsent(event.getUsername(), k -> new ArrayDeque<>());
        addAndTrim(userDeque, ts, DetectionConfig.USER_WINDOW);
        if (userDeque.size() == DetectionConfig.USER_FAIL_THRESHOLD) {
            alertService.alertUserBruteForce(event.getUsername(), userDeque.size(), DetectionConfig.USER_WINDOW_LABEL, ts);
        }

        Deque<Instant> failSuccessDeque = failSuccessByUser.computeIfAbsent(event.getUsername(), k -> new ArrayDeque<>());
        addAndTrim(failSuccessDeque, ts, DetectionConfig.FAIL_SUCCESS_WINDOW);
    }

    private void handleSuccess(LoginEvent event) {
        Instant ts = event.getTimestamp();
        Deque<Instant> failDeque = failSuccessByUser.get(event.getUsername());
        if (failDeque != null) {
            trim(failDeque, ts, DetectionConfig.FAIL_SUCCESS_WINDOW);
            if (failDeque.size() >= DetectionConfig.FAIL_SUCCESS_THRESHOLD) {
                alertService.alertFailToSuccess(
                        event.getUsername(),
                        event.getIpAddress(),
                        failDeque.size(),
                        DetectionConfig.FAIL_SUCCESS_WINDOW_LABEL,
                        ts
                );
            }
            failDeque.clear();
        }
    }

    private void addAndTrim(Deque<Instant> deque, Instant ts, Duration window) {
        deque.addLast(ts);
        trim(deque, ts, window);
    }

    private void trim(Deque<Instant> deque, Instant pivot, Duration window) {
        Instant cutoff = pivot.minus(window);
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.removeFirst();
        }
    }
}
