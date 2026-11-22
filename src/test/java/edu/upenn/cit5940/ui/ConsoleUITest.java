package edu.upenn.cit5940.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.cit5940.datamanagement.StubArticleRepository;
import edu.upenn.cit5940.logging.AppLogger;
import edu.upenn.cit5940.processor.SearchEngineService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Checks the console output against the keywords in the UI specification. */
class ConsoleUITest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppLogger.init(tempDir.resolve("ui.log").toString());
    }

    /** Runs the UI with the given input lines; returns lower-cased output. */
    private String run(String... inputLines) {
        String input = String.join("\n", inputLines) + "\n";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ConsoleUI ui = new ConsoleUI(
                new SearchEngineService(new StubArticleRepository()),
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buf, true, StandardCharsets.UTF_8));
        ui.showStartup("data/test.csv");
        ui.run();
        return buf.toString(StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    private void has(String out, String... words) {
        for (String w : words) {
            assertTrue(out.contains(w), "missing \"" + w + "\" in:\n" + out);
        }
    }

    @Test
    void startupMessages() {
        String out = run("4");
        has(out, "tech news search engine", "initializing", "architecture",
                "loading articles from: data/test.csv", "5 articles loaded",
                "initialization", "complete");
    }

    @Test
    void mainMenu() {
        String out = run("4");
        has(out, "main menu", "1. interactive", "2. command", "3. help", "4. exit",
                "please select an option (1-4):");
    }

    @Test
    void helpScreen() {
        String out = run("3", "4");
        has(out, "help & documentation", "interactive mode", "command mode",
                "available services", "search articles", "autocomplete");
    }

    @Test
    void commandModeShell() {
        String out = run("2", "menu", "4");
        has(out, "command mode", "enter commands directly",
                "type 'help' for available commands", "type 'menu' to return", "\n> ");
    }

    @Test
    void commandModeHelpListsAllCommands() {
        String out = run("2", "help", "menu", "4");
        has(out, "search <keyword>", "autocomplete <prefix>", "topics <period>",
                "trends <topic>", "articles <start_date>", "article <id>",
                "stats", "help", "menu");
    }

    @Test
    void commandModeErrors() {
        String out = run("2", "bogus", "search", "menu", "4");
        has(out, "unknown command", "usage: search <keyword>");
    }

    @Test
    void menuInputErrors() {
        String out = run("", "abc", "9", "4");
        has(out, "please enter a choice", "please enter a valid number", "invalid choice");
    }

    @Test
    void exitMessages() {
        has(run("4"), "thank you", "goodbye");
    }

    @Test
    void endOfInputStillExitsCleanly() {
        has(run(), "thank you", "goodbye");
    }

    @Test
    void interactiveModeShowsSpecKeywordsAndReturns() {
        String out = run("1", "", "ai", "back", "4");
        has(out, "interactive mode", "available services", "search articles",
                "enter search keyword", "search results", "please enter a search keyword");
        // after 'back' the main menu is shown again, then exit
        assertTrue(out.indexOf("main menu", out.indexOf("interactive mode")) > 0, out);
        has(out, "goodbye");
    }

    @Test
    void commandNamesAreCaseInsensitive() {
        String out = run("2", "HELP", "MENU", "4");
        has(out, "available commands:");
        assertTrue(!out.contains("unknown command"), out);
    }

    @Test
    void blankCommandLineIsIgnored() {
        String out = run("2", "", "   ", "menu", "4");
        assertTrue(!out.contains("unknown command"), out);
    }

    @Test
    void searchWithKeywordDoesNotShowUsage() {
        String out = run("2", "search ai", "menu", "4");
        assertTrue(!out.contains("usage: search"), out);
    }

    @Test
    void unimplementedCommandsAreRecognisedNotUnknown() {
        String out = run("2", "stats", "topics 2022-07", "menu", "4");
        assertTrue(!out.contains("unknown command"), out);
        has(out, "not available yet");
    }

    @Test
    void menuChoiceWithSurroundingSpacesIsAccepted() {
        has(run("  4  "), "goodbye");
    }

    @Test
    void returnsToMainMenuAfterEachScreen() {
        String out = run("3", "2", "menu", "1", "back", "4");
        // initial menu + after help + after command mode + after interactive = 4 menus
        int count = out.split("1\\. interactive", -1).length - 1;
        assertTrue(count == 4, "menus shown: " + count + "\n" + out);
    }

    @Test
    void emptyInputMessage() {
        has(run("", "4"), "please enter a choice");
    }

    @Test
    void whitespaceOnlyCountsAsEmpty() {
        has(run("   ", "4"), "please enter a choice");
    }

    @Test
    void nonNumericInputMessages() {
        for (String bad : new String[] {"abc", "1.5", "one", "1a", "!"}) {
            String out = run(bad, "4");
            has(out, "please enter a valid number");
            assertTrue(!out.contains("invalid choice"), bad + "\n" + out);
        }
    }

    @Test
    void outOfRangeNumbersAreInvalidChoice() {
        for (String bad : new String[] {"0", "5", "-1", "99999999999"}) {
            String out = run(bad, "4");
            has(out, "invalid choice");
            assertTrue(!out.contains("valid number"), bad + "\n" + out);
        }
    }

    @Test
    void errorsRepromptUntilValidChoice() {
        String out = run("", "abc", "7", "4");
        has(out, "please enter a choice", "please enter a valid number",
                "invalid choice", "goodbye");
    }

    @Test
    void unknownCommandMessageEndsWithPeriod() {
        has(run("2", "zzz", "menu", "4"), "unknown command. type 'help' for available commands.");
    }
}
