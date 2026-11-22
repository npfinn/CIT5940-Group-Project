package edu.upenn.cit5940.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppLoggerTest {

    @TempDir
    Path tempDir;

    private Path logFile;

    @BeforeEach
    void setUp() {
        logFile = tempDir.resolve("test.log");
        AppLogger.init(logFile.toString());
    }

    @AfterEach
    void tearDown() {
        AppLogger.getInstance().close();
        // reset singleton to a throwaway path so other tests don't leak state
        AppLogger.init(tempDir.resolve("reset.log").toString());
        AppLogger.getInstance().close();
    }

    private List<String> lines() throws IOException {
        return Files.readAllLines(logFile);
    }

    @Test
    void getInstanceReturnsSameObject() {
        assertSame(AppLogger.getInstance(), AppLogger.getInstance());
    }

    @Test
    void infoLineHasTimestampLevelAndMessage() throws IOException {
        AppLogger.getInstance().info("Application starting");
        List<String> lines = lines();
        assertEquals(1, lines.size());
        assertTrue(Pattern.matches(
                "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] INFO  Application starting",
                lines.get(0)), lines.get(0));
    }

    @Test
    void warnAndErrorLevels() throws IOException {
        AppLogger.getInstance().warn("careful");
        AppLogger.getInstance().error("Failed to parse record at line 1023");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("] WARN  careful"));
        assertTrue(lines.get(1).contains("] ERROR Failed to parse record at line 1023"));
    }

    @Test
    void errorWithThrowableIncludesException() throws IOException {
        AppLogger.getInstance().error("boom", new IllegalStateException("bad"));
        assertTrue(lines().get(0).contains("boom: java.lang.IllegalStateException: bad"));
    }

    @Test
    void errorWithNullThrowableIsJustMessage() throws IOException {
        AppLogger.getInstance().error("boom", null);
        assertTrue(lines().get(0).endsWith("ERROR boom"));
    }

    @Test
    void appendsToExistingFileOnReinit() throws IOException {
        AppLogger.getInstance().info("first");
        AppLogger.init(logFile.toString());
        AppLogger.getInstance().info("second");
        List<String> lines = lines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("first"));
        assertTrue(lines.get(1).endsWith("second"));
    }

    @Test
    void initSwitchesToNewFile() throws IOException {
        Path other = tempDir.resolve("other.log");
        AppLogger.init(other.toString());
        AppLogger.getInstance().info("elsewhere");
        assertTrue(Files.readAllLines(other).get(0).endsWith("elsewhere"));
        assertFalse(Files.exists(logFile) && !Files.readAllLines(logFile).isEmpty());
    }

    @Test
    void createsMissingParentDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c.log");
        AppLogger.init(nested.toString());
        AppLogger.getInstance().info("nested");
        assertTrue(Files.exists(nested));
    }

    @Test
    void doesNotWriteToConsole() {
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out));
            System.setErr(new PrintStream(err));
            AppLogger.getInstance().info("quiet");
            AppLogger.getInstance().error("quiet error", new RuntimeException("x"));
        } finally {
            System.setOut(origOut);
            System.setErr(origErr);
        }
        assertEquals(0, out.size());
        assertEquals(0, err.size());
    }

    @Test
    void logAfterCloseIsSilentNoOp() throws IOException {
        AppLogger.getInstance().close();
        assertDoesNotThrow(() -> AppLogger.getInstance().info("ignored"));
        assertTrue(lines().isEmpty());
    }

    @Test
    void unwritablePathDoesNotThrow() throws IOException {
        Path blocker = Files.createFile(tempDir.resolve("file"));
        // parent "directory" is a regular file, so opening must fail
        assertDoesNotThrow(() -> AppLogger.init(blocker.resolve("x.log").toString()));
        assertDoesNotThrow(() -> AppLogger.getInstance().info("nowhere"));
    }

    @Test
    void nullOrBlankPathFallsBackToDefault() {
        // default "app.log" is relative to cwd; just verify no exception and clean up
        Path def = Path.of("app.log");
        boolean existed = Files.exists(def);
        try {
            assertDoesNotThrow(() -> AppLogger.init(null));
            assertDoesNotThrow(() -> AppLogger.init("  "));
        } finally {
            AppLogger.getInstance().close();
            if (!existed) {
                try { Files.deleteIfExists(def); } catch (IOException ignored) { }
            }
        }
    }

    @Test
    void concurrentLoggingProducesWholeLines() throws Exception {
        int threads = 8, per = 50;
        Thread[] ts = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int id = i;
            ts[i] = new Thread(() -> {
                for (int j = 0; j < per; j++) {
                    AppLogger.getInstance().info("t" + id + "-" + j);
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        List<String> lines = lines();
        assertEquals(threads * per, lines.size());
        Pattern p = Pattern.compile("\\[.{19}\\] INFO  t\\d-\\d+");
        for (String l : lines) assertTrue(p.matcher(l).matches(), l);
    }
}
