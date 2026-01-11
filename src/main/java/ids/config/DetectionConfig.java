package ids.config;

import java.time.Duration;

public final class DetectionConfig {
    private DetectionConfig() {
        // no instances
    }

    public static final int IP_FAIL_THRESHOLD = 5;
    public static final Duration IP_WINDOW = Duration.ofMinutes(5);
    public static final String IP_WINDOW_LABEL = "5m";

    public static final int USER_FAIL_THRESHOLD = 10;
    public static final Duration USER_WINDOW = Duration.ofMinutes(10);
    public static final String USER_WINDOW_LABEL = "10m";

    public static final int FAIL_SUCCESS_THRESHOLD = 3;
    public static final Duration FAIL_SUCCESS_WINDOW = Duration.ofMinutes(2);
    public static final String FAIL_SUCCESS_WINDOW_LABEL = "2m";

    public static final boolean DEBUG_PARSE_DEFAULT = false;
}
