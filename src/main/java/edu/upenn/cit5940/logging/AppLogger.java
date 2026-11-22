package edu.upenn.cit5940.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Singleton design file logger. all output goes to a log file
 * Line format: [yyyy-MM-dd HH:mm:ss] LEVEL message
 *
 * Usage:
 *   AppLogger.init("app.log");          // once at startup (optional; default is "app.log")
 *   AppLogger.getInstance().info("Application starting");
 */
public final class AppLogger {

    public enum Level { INFO, WARN, ERROR }

    private static final String DEFAULT_LOG_FILE = "app.log";
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static AppLogger instance;
    private static String configuredPath = DEFAULT_LOG_FILE;

    private BufferedWriter writer;
    private final Path logPath;

    private AppLogger(String path) {
        this.logPath = Paths.get(path);
        open();
    }

    /**
     * Sets the log file path and must be called before first getInstacne()
     * if the logger already exists it is repointed to the new file
     */
    public static synchronized void init(String path) {
        if (path == null || path.isBlank()) {
            path = DEFAULT_LOG_FILE;
        }
        configuredPath = path;
        if (instance != null) {
            instance.close();
            instance = null;
        }
        getInstance();
    }

    // returns the global logger instance, creating it on first use
    public static synchronized AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger(configuredPath);
        }
        return instance;
    }

    private void open() {
        try {
            Path parent = logPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            writer = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            writer = null; // logging failures must never crash the app or print to console
        }
    }

    public synchronized void log(Level level, String message) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(String.format("[%s] %-5s %s",
                    LocalDateTime.now().format(TS_FORMAT), level, message));
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
            // intentionally silent: no console output allowed
        }
    }

    // convenience methods for each log level
    public void info(String message)  { log(Level.INFO, message); }

    public void warn(String message)  { log(Level.WARN, message); }

    public void error(String message) { log(Level.ERROR, message); }

    public void error(String message, Throwable t) {
        log(Level.ERROR, message + (t == null ? "" : ": " + t));
    }

    // close the log file, if open. must be called at shutdown to flush and release the file
    public synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
                // ignore
            }
            writer = null;
        }
    }
}
