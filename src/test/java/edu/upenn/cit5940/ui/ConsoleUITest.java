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

    /** Stub whose autocomplete returns canned words (the real stub returns none). */
    private static class AutocompleteStub extends StubArticleRepository {
        @Override
        public java.util.List<String> autocomplete(String prefix, int limit) {
            java.util.List<String> all = java.util.List.of("tech", "technology", "tesla", "telescope",
                    "teams", "tea", "ten", "tennis", "tent", "term", "terms", "test");
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String w : all) {
                if (w.startsWith(prefix.toLowerCase()) && out.size() < limit) {
                    out.add(w);
                }
            }
            return out;
        }
    }

    /** Stub with canned word counts and trend values. */
    private static class TopicStub extends StubArticleRepository {
        @Override
        public java.util.Map<String, Integer> getWordFrequencies(java.time.YearMonth period) {
            java.util.Map<String, Integer> m = new java.util.HashMap<>();
            m.put("twitter", 9);
            m.put("musk", 7);
            m.put("apple", 7);
            return m;
        }

        @Override
        public java.util.SortedMap<java.time.YearMonth, Integer> getTopicTrend(
                String topic, java.time.YearMonth start, java.time.YearMonth end) {
            java.util.SortedMap<java.time.YearMonth, Integer> m = new java.util.TreeMap<>();
            int n = 0;
            for (java.time.YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
                m.put(ym, n += 2);
            }
            return m;
        }
    }

    private String run(String... inputLines) {
        return run(new StubArticleRepository(), inputLines);
    }

    /** Runs the UI with the given repository and input lines; returns lower-cased output. */
    private String run(edu.upenn.cit5940.datamanagement.ArticleRepository repo, String... inputLines) {
        String input = String.join("\n", inputLines) + "\n";
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ConsoleUI ui = new ConsoleUI(
                new SearchEngineService(repo),
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buf, true, StandardCharsets.UTF_8));
        ui.showStartup("data/test.csv");
        ui.run();
        
        //this was failing because windows adds \r\n, so added \n to match your checks
        return buf.toString(StandardCharsets.UTF_8).replace("\r\n", "\n").toLowerCase(Locale.ROOT);
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
        String out = run("1", "1", "tesla", "", "8", "4");
        has(out, "interactive mode", "available services", "1. search articles",
                "2. get autocomplete suggestions", "3. view top topics", "4. analyze topic trends",
                "5. browse articles by date", "6. view specific article by id",
                "7. show statistics", "8. back to main menu", "select a service (1-8):",
                "enter search keyword", "search results", "press enter");
        // after service 8 the main menu is shown again, then exit
        assertTrue(out.indexOf("main menu", out.indexOf("8. back to main menu")) > 0, out);
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
        // every spec command is recognised, none are "unknown"
        String out = run("2", "search a", "autocomplete a", "topics 2022-07", "trends a 2022-07 2022-07",
                "articles 2022-07-01 2022-07-02", "article 1", "stats", "menu", "4");
        assertTrue(!out.contains("unknown command"), out);
        assertTrue(!out.contains("not available yet"), out);
    }

    @Test
    void menuChoiceWithSurroundingSpacesIsAccepted() {
        has(run("  4  "), "goodbye");
    }

    @Test
    void returnsToMainMenuAfterEachScreen() {
        String out = run("3", "2", "menu", "1", "8", "4");
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

    @Test
    void searchPrintsHeaderAndMatchingTitles() {
        String out = run("2", "search twitter", "menu", "4");
        has(out, "search results for 'twitter'", "twitter board vows legal action",
                "twitter trial date set");
        assertTrue(!out.contains("apple unveils"), out);
    }

    @Test
    void searchWithSeveralKeywordsIsAnd() {
        String out = run("2", "search twitter musk", "menu", "4");
        has(out, "twitter board vows legal action");
        assertTrue(!out.contains("twitter trial date set"), out);
    }

    @Test
    void searchIsCaseInsensitive() {
        has(run("2", "SEARCH TWITTER", "menu", "4"), "twitter board vows legal action");
    }

    @Test
    void searchWithNoMatchesSaysNoArticlesFound() {
        has(run("2", "search zzzzz", "menu", "4"), "no articles found");
    }

    @Test
    void searchWithoutKeywordShowsUsage() {
        has(run("2", "search", "search   ", "menu", "4"), "usage: search <keyword>");
    }

    @Test
    void autocompleteShowsSuggestionsHeaderAndWords() {
        String out = run(new AutocompleteStub(), "2", "autocomplete tech", "menu", "4");
        has(out, "autocomplete suggestions for 'tech'", "technology");
    }

    @Test
    void autocompleteShowsAtMostTenSuggestions() {
        // 12 words start with "t"; only the first 10 may be shown
        String out = run(new AutocompleteStub(), "2", "autocomplete t", "menu", "4");
        has(out, "\ntennis\n", "\nterm\n");
        assertTrue(!out.contains("\nterms\n") && !out.contains("\ntest\n"), out);
    }

    @Test
    void autocompleteWithNoMatchesSaysNoSuggestions() {
        has(run(new AutocompleteStub(), "2", "autocomplete qq", "menu", "4"), "no suggestions found");
    }

    @Test
    void autocompleteWithoutPrefixShowsUsage() {
        has(run("2", "autocomplete", "menu", "4"), "usage: autocomplete <prefix>");
    }

    @Test
    void interactiveModeSearchesForReal() {
        String out = run("1", "1", "tesla", "", "8", "4");
        has(out, "search results for 'tesla'", "tesla robotaxi plans delayed");
    }

    @Test
    void articlesListsTitlesChronologically() {
        String out = run("2", "articles 2022-07-01 2022-08-31", "menu", "4");
        has(out, "articles from 2022-07-01 to 2022-08-31", "twitter board vows legal action");
        assertTrue(out.indexOf("twitter board vows") < out.indexOf("apple unveils")
                && out.indexOf("apple unveils") < out.indexOf("tesla robotaxi"), out);
        assertTrue(!out.contains("twitter trial date set"), out);
    }

    @Test
    void articlesRangeIsInclusive() {
        String out = run("2", "articles 2022-07-15 2022-07-15", "menu", "4");
        has(out, "apple unveils new chip", "google expands cloud region");
    }

    @Test
    void articlesWithNoMatchSaysNoArticlesFound() {
        has(run("2", "articles 2000-01-01 2000-01-31", "menu", "4"), "no articles found");
    }

    @Test
    void articlesInvalidDateShowsAppendixError() {
        for (String cmd : new String[] {"articles 2022-04-31 2022-05-01", "articles abc 2022-05-01",
                "articles 2022-1-1 2022-05-01"}) {
            String out = run("2", cmd, "menu", "4");
            has(out, "invalid date provided", "yyyy-mm-dd");
        }
    }

    @Test
    void articlesStartAfterEndIsError() {
        String out = run("2", "articles 2022-09-01 2022-07-01", "menu", "4");
        has(out, "invalid", "date");
        assertTrue(!out.contains("articles from 2022"), out); // no results header on error
    }

    @Test
    void articlesWithWrongArgumentCountShowsUsage() {
        has(run("2", "articles", "articles 2022-07-01", "articles a b c", "menu", "4"),
                "usage: articles <start_date> <end_date>");
    }

    @Test
    void articleShowsDetails() {
        String out = run("2", "article 2", "menu", "4");
        has(out, "article details", "id: 2", "date: 2022-07-15", "title: apple unveils new chip",
                "apple announced a faster chip");
    }

    @Test
    void articleNotFound() {
        has(run("2", "article 999", "menu", "4"), "article not found: 999");
    }

    @Test
    void articleWithoutIdShowsUsage() {
        has(run("2", "article", "menu", "4"), "usage: article <id>");
    }

    @Test
    void errorsReturnToThePrompt() {
        // after a bad articles command the shell still works
        String out = run("2", "articles bad bad", "article 1", "menu", "4");
        has(out, "invalid date provided", "article details", "goodbye");
    }

    @Test
    void topicsShowsRankedWords() {
        String out = run(new TopicStub(), "2", "topics 2022-07", "menu", "4");
        has(out, "top topics for 2022-07", "1. twitter (9)", "2. apple (7)", "3. musk (7)");
    }

    @Test
    void topicsWithNoDataSaysNoTopicsFound() {
        has(run("2", "topics 2022-07", "menu", "4"), "top topics for 2022-07", "no topics found");
    }

    @Test
    void topicsInvalidPeriodShowsError() {
        for (String bad : new String[] {"2024-13", "2024-1", "abc", "2024-01-05"}) {
            has(run("2", "topics " + bad, "menu", "4"), "invalid date provided", "yyyy-mm");
        }
    }

    @Test
    void topicsWithWrongArgumentCountShowsUsage() {
        has(run("2", "topics", "topics 2022-07 2022-08", "menu", "4"), "usage: topics <period>");
    }

    @Test
    void trendsListsEachMonthInRange() {
        String out = run(new TopicStub(), "2", "trends twitter 2022-07 2022-09", "menu", "4");
        has(out, "trend for topic 'twitter' from 2022-07 to 2022-09",
                "2022-07: 2", "2022-08: 4", "2022-09: 6");
    }

    @Test
    void trendsAgainstPlainStubShowsZeroCounts() {
        has(run("2", "trends twitter 2022-07 2022-08", "menu", "4"), "2022-07: 0", "2022-08: 0");
    }

    @Test
    void trendsInvalidPeriodOrOrderShowsError() {
        has(run("2", "trends ai 2022-13 2022-14", "menu", "4"), "invalid date provided");
        has(run("2", "trends ai 2022-09 2022-07", "menu", "4"), "invalid date range");
    }

    @Test
    void trendsWithWrongArgumentCountShowsUsage() {
        has(run("2", "trends", "trends ai 2022-07", "trends a b c d", "menu", "4"),
                "usage: trends <topic> <start> <end>");
    }

    @Test
    void shellKeepsWorkingAfterTopicsAndTrendsErrors() {
        String out = run("2", "topics bad", "trends ai bad bad", "search twitter", "menu", "4");
        has(out, "invalid date provided", "twitter board vows legal action", "goodbye");
    }

    @Test
    void statsCommandShowsCountAndDateRange() {
        String out = run("2", "stats", "menu", "4");
        has(out, "database statistics", "total articles: 5",
                "earliest article date: 2022-07-08", "latest article date: 2022-09-20");
    }

    // ---- interactive mode: all 8 services, against the stub ----

    @Test
    void interactiveAutocomplete() {
        String out = run(new AutocompleteStub(), "1", "2", "tech", "", "8", "4");
        has(out, "enter a prefix", "autocomplete suggestions for 'tech'", "technology");
    }

    @Test
    void interactiveTopTopics() {
        String out = run(new TopicStub(), "1", "3", "2022-07", "", "8", "4");
        has(out, "enter a period", "top topics for 2022-07", "1. twitter (9)");
    }

    @Test
    void interactiveTopTopicsInvalidPeriod() {
        has(run("1", "3", "2024-13", "", "8", "4"), "invalid date provided");
    }

    @Test
    void interactiveTrends() {
        String out = run(new TopicStub(), "1", "4", "twitter", "2022-07", "2022-08", "", "8", "4");
        has(out, "enter a topic word", "enter the start period", "enter the end period",
                "trend for topic 'twitter' from 2022-07 to 2022-08", "2022-07: 2", "2022-08: 4");
    }

    @Test
    void interactiveTrendsRejectsBadOrder() {
        has(run("1", "4", "ai", "2022-09", "2022-07", "", "8", "4"), "invalid date range");
    }

    @Test
    void interactiveBrowseByDate() {
        String out = run("1", "5", "2022-07-01", "2022-08-31", "", "8", "4");
        has(out, "enter the start date", "enter the end date",
                "articles from 2022-07-01 to 2022-08-31", "apple unveils new chip");
    }

    @Test
    void interactiveBrowseByDateInvalid() {
        has(run("1", "5", "2022-04-31", "2022-05-01", "", "8", "4"), "invalid date provided");
    }

    @Test
    void interactiveViewArticle() {
        String out = run("1", "6", "3", "", "8", "4");
        has(out, "enter the article id", "article details", "title: google expands cloud region");
    }

    @Test
    void interactiveViewArticleNotFound() {
        has(run("1", "6", "999", "", "8", "4"), "article not found: 999");
    }

    @Test
    void interactiveStatistics() {
        String out = run("1", "7", "", "8", "4");
        has(out, "database statistics", "total articles: 5");
    }

    @Test
    void interactiveMenuInputErrorsUseOneToEight() {
        String out = run("1", "", "abc", "9", "0", "8", "4");
        has(out, "please enter a choice (1-8)", "please enter a valid number (1-8)",
                "invalid choice. please enter 1-8");
    }

    @Test
    void interactiveMenuAcceptsFiveToEightButMainMenuDoesNot() {
        // 5-8 are valid in the interactive menu...
        has(run("1", "8", "4"), "goodbye");
        // ...but 5 is still an invalid choice on the main menu
        has(run("5", "4"), "invalid choice. please enter 1-4");
    }

    @Test
    void interactiveReturnsToServiceMenuAfterEnter() {
        String out = run("1", "7", "", "7", "", "8", "4");
        int menus = out.split("select a service \\(1-8\\):", -1).length - 1;
        assertTrue(menus == 3, "service menus shown: " + menus + "\n" + out);
    }

    @Test
    void interactiveEndOfInputMidPromptExitsCleanly() {
        has(run("1", "4", "ai"), "thank you", "goodbye");
        has(run("1", "5"), "goodbye");
        has(run("1", "1", "tesla"), "goodbye"); // ends at "Press ENTER"
    }
}
