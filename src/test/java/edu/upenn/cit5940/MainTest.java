package edu.upenn.cit5940;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.cit5940.logging.AppLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path tempDir;

    /** Runs Main with the given args and stdin; returns what it printed. */
    private String runMain(String stdin, String... args) {
        InputStream origIn = System.in;
        PrintStream origOut = System.out;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(buf, true, StandardCharsets.UTF_8));
            Main.main(args);
        } finally {
            System.setIn(origIn);
            System.setOut(origOut);
            AppLogger.getInstance().close();
        }
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void usesGivenDataAndLogPaths() throws Exception {
        Path log = tempDir.resolve("run.log");
        String out = runMain("4\n", "data/articles_small.csv", log.toString());

        assertTrue(out.contains("Loading articles from: data/articles_small.csv"), out);
        assertTrue(out.contains("Goodbye!"), out);
        String logText = Files.readString(log);
        assertTrue(logText.contains("Application starting"), logText);
        assertTrue(logText.contains("Loaded 100 articles from data/articles_small.csv"), logText);
        assertTrue(logText.contains("Application exiting"), logText);
    }

    @Test
    void logsNothingToConsole() {
        String out = runMain("4\n", "data/articles_small.csv", tempDir.resolve("q.log").toString());
        assertTrue(!out.contains("INFO") && !out.contains("ERROR"), out);
    }
}
