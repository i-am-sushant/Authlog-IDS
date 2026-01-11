package ids.cli;

import ids.alert.AlertService;
import ids.config.DetectionConfig;
import ids.detect.DetectionEngine;
import ids.model.LoginEvent;
import ids.parser.AuthLogParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public final class App {
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        boolean debugParse = DetectionConfig.DEBUG_PARSE_DEFAULT;
        String filePath;

        if ("--debug-parse".equals(args[0])) {
            debugParse = true;
            if (args.length < 2) {
                printUsage();
                System.exit(1);
                return;
            }
            filePath = args[1];
        } else {
            filePath = args[0];
        }

        AuthLogParser parser = new AuthLogParser(debugParse);
        AlertService alertService = new AlertService();
        DetectionEngine engine = new DetectionEngine(alertService);

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            System.err.println("File not found: " + filePath);
            System.exit(1);
        }

        try (Stream<String> lines = Files.lines(path)) {
            lines.forEach(line -> {
                Optional<LoginEvent> eventOpt = parser.parse(line);
                eventOpt.ifPresent(engine::process);
            });
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp out ids.cli.App [--debug-parse] <auth_log_file>");
    }
}
