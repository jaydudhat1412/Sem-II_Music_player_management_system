import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtility {
    private static final String LOG_FILE = "system_activity.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String formattedLog = String.format("[%s] [%s] %s", timestamp, level, message);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(formattedLog);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to log file: " + e.getMessage());
        }
    }

    public static void logInfo(String message) {
        log("INFO", message);
    }

    public static void logWarning(String message) {
        log("WARNING", message);
    }

    public static void logError(String message) {
        log("ERROR", message);
    }
}
