package ids.parser;

import ids.model.EventType;
import ids.model.LoginEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AuthLogParser {
    private static final Pattern SSHD_PATTERN = Pattern.compile(
            "^(\\w{3})\\s+(\\d{1,2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+[^ ]+\\s+sshd\\[[^]]+\\]:\\s+(Failed|Accepted) password for (?:invalid user )?(\\S+) from ([0-9]{1,3}(?:\\.[0-9]{1,3}){3}).*$"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy MMM d HH:mm:ss", Locale.ENGLISH);

    private final boolean debugParse;

    public AuthLogParser(boolean debugParse) {
        this.debugParse = debugParse;
    }

    public Optional<LoginEvent> parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = SSHD_PATTERN.matcher(line);
        if (!matcher.matches()) {
            warn("unrecognized format", line);
            return Optional.empty();
        }

        String month = matcher.group(1);
        String day = matcher.group(2);
        String time = matcher.group(3);
        String outcome = matcher.group(4);
        String user = matcher.group(5);
        String ip = matcher.group(6);

        EventType type = outcome.equalsIgnoreCase("Failed") ? EventType.FAIL : EventType.SUCCESS;

        Instant instant;
        try {
            int currentYear = LocalDateTime.now().getYear();
            LocalDateTime localDateTime = LocalDateTime.parse(
                    currentYear + " " + month + " " + day + " " + time,
                    TIME_FORMATTER
            );
            instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ex) {
            warn("timestamp parse failed", line);
            return Optional.empty();
        }

        LoginEvent event = new LoginEvent(instant, user, ip, type);
        return Optional.of(event);
    }

    private void warn(String reason, String line) {
        if (debugParse) {
            System.err.println("WARN parse-skip: " + reason + " line=\"" + line + "\"");
        }
    }
}
