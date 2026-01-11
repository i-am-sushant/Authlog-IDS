package ids.model;

import java.time.Instant;
import java.util.Objects;

public final class LoginEvent {
    private final Instant timestamp;
    private final String username;
    private final String ipAddress;
    private final EventType eventType;

    public LoginEvent(Instant timestamp, String username, String ipAddress, EventType eventType) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.username = Objects.requireNonNull(username, "username");
        this.ipAddress = Objects.requireNonNull(ipAddress, "ipAddress");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public EventType getEventType() {
        return eventType;
    }
}
