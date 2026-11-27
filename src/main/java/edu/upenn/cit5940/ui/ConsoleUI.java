package edu.upenn.cit5940.ui;

import edu.upenn.cit5940.common.dto.Article;
import edu.upenn.cit5940.logging.AppLogger;
import edu.upenn.cit5940.processor.DatabaseStats;
import edu.upenn.cit5940.processor.InvalidInputException;
import edu.upenn.cit5940.processor.SearchEngineService;
import edu.upenn.cit5940.processor.TopicCount;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.SortedMap;

/**
 * presentation: startup messages, main menu and mode navigation.
 * Input and output streams are injected so the UI can be tested.
 */
public class ConsoleUI {

    // separator line used around menu headers
    private static final String LINE = "=".repeat(50);

    // Error messages required by Appendix A (the autograder matches keywords):
    //   invalid choice -> "invalid" + "choice"
    //   empty input    -> "please" + "enter" + "choice"
    //   non-numeric    -> "please" + "enter" + "valid" + "number"
    // (%d is the highest valid choice: 4 for the main menu, 8 for the interactive menu)
    private static final String MSG_INVALID_CHOICE = "Invalid choice. Please enter 1-%d: ";
    private static final String MSG_EMPTY_INPUT = "Please enter a choice (1-%d): ";
    private static final String MSG_NOT_A_NUMBER = "Please enter a valid number (1-%d): ";

    // processor-tier service: the UI never talks to the data tier directly 
    private final SearchEngineService service;
    // reads user input line by line
    private final Scanner in;
    // all user-facing output goes here (System.out in production)
    private final PrintStream out;
    // global logger: UI events go to the log file, never to the console
    private final AppLogger log = AppLogger.getInstance();

    /**
     * @param service processor-tier service (injected by Main)
     * @param input   where user input is read from
     * @param output  where all console output is written
     */
    public ConsoleUI(SearchEngineService service, InputStream input, PrintStream output) {
        this.service = service;
        this.in = new Scanner(input);
        this.out = output;
    }

    /**
     * prints the startup messages required by the UI spec: title, initializing,
     * loading path, article count and initialization complete.
     *
     * @param dataPath the data file path shown in the "Loading articles from" line
     */
    public void showStartup(String dataPath) {
        out.println("=== Tech News Search Engine ===");
        out.println("Initializing n-tier architecture...");
        out.println("Loading articles from: " + dataPath);
        out.println(service.getArticleCount() + " articles loaded");
        out.println("Architecture initialization complete!");
        out.println();
    }

    /**
     * Runs the main menu loop: show the menu, read a choice, dispatch to the
     * chosen screen, and repeat until the user picks Exit or input ends.
     */
    public void run() {
        boolean running = true;
        while (running) {
            showMainMenu();
            Integer choice = readMenuChoice(4);
            if (choice == null) { // input ended 
                break;
            }
            log.info("Main menu choice: " + choice);
            // each mode returns here when the user leaves it and then menu shows again
            switch (choice) {
                case 1 -> interactiveMode();
                case 2 -> commandMode();
                case 3 -> showHelp();
                case 4 -> running = false;
                default -> { }
            }
        }
        showExit();
    }

    // Prints the main menu and the input prompt (no newline after the prompt)
    private void showMainMenu() {
        out.println(LINE);
        out.println("MAIN MENU");
        out.println(LINE);
        out.println("1. Interactive Mode (Guided Menu)");
        out.println("2. Command Mode (Direct Commands)");
        out.println("3. Help & Documentation");
        out.println("4. Exit");
        out.println(LINE);
        out.print("Please select an option (1-4): ");
        out.flush();
    }

    /**
     * Reads a valid choice 1-4, re-prompting with the appropriate error
     * message. returns null if input ends
     */
    private Integer readMenuChoice(int max) {
        while (true) {
            if (!in.hasNextLine()) {
                return null;
            }
            String line = in.nextLine().trim();
            String error = validateMenuInput(line, max);
            if (error == null) {
                return Integer.parseInt(line);
            }
            // reprompt with the specific error so the user can try again.
            out.print(error);
            out.flush();
        }
    }

    /**
     * checks one line of main menu input
     *
     * @return null if the line is a valid choice (1-4); otherwise the error
     *         message to show
     */
    private String validateMenuInput(String line, int max) {
        if (line.isEmpty()) {
            log.warn("Empty menu input");
            return String.format(MSG_EMPTY_INPUT, max);
        }
        // whole numbers (including negatives and ones too large for an int)
        // count as numeric; anything else like abc" or "1.5", does not.
        if (!line.matches("[+-]?\\d+")) {
            log.warn("Non-numeric menu input: \"" + line + "\"");
            return String.format(MSG_NOT_A_NUMBER, max);
        }
        if (line.matches("[+]?\\d{1,3}") && Integer.parseInt(line.replace("+", "")) >= 1
                && Integer.parseInt(line.replace("+", "")) <= max) {
            return null;
        }
        log.warn("Invalid menu choice: " + line);
        return String.format(MSG_INVALID_CHOICE, max);
    }

    /**
     * Guided mode: shows the 8-service menu, asks for each input step by step,
     * runs the same handlers as Command Mode, then waits for ENTER before
     * showing the menu again. Service 8 returns to the main menu.
     */
    private void interactiveMode() {
        while (true) {
            showInteractiveMenu();
            Integer choice = readMenuChoice(8);
            if (choice == null || choice == 8) { // input ended, or "Back to Main Menu"
                return;
            }
            log.info("Interactive service: " + choice);
            if (!runInteractiveService(choice)) {
                return; // input ended while answering a prompt
            }
            out.print("Press ENTER to return to the Interactive Mode menu...");
            out.flush();
            if (!in.hasNextLine()) {
                return;
            }
            in.nextLine();
        }
    }

    private void showInteractiveMenu() {
        out.println(LINE);
        out.println("INTERACTIVE MODE");
        out.println(LINE);
        out.println("This mode will guide you through each operation step by step.");
        out.println("-".repeat(40));
        out.println("AVAILABLE SERVICES");
        out.println("-".repeat(40));
        out.println("1. Search Articles");
        out.println("2. Get Autocomplete Suggestions");
        out.println("3. View Top Topics");
        out.println("4. Analyze Topic Trends");
        out.println("5. Browse Articles by Date");
        out.println("6. View Specific Article by ID");
        out.println("7. Show Statistics");
        out.println("8. Back to Main Menu");
        out.println("-".repeat(40));
        out.print("Select a service (1-8): ");
        out.flush();
    }

    /**
     * Asks for the inputs of one service and runs it.
     *
     * @return false if input ended before the service could run
     */
    private boolean runInteractiveService(int choice) {
        switch (choice) {
            case 1 -> {
                String q = ask("Enter search keyword(s): ");
                if (q == null) return false;
                handleSearch(q);
            }
            case 2 -> {
                String prefix = ask("Enter a prefix: ");
                if (prefix == null) return false;
                handleAutocomplete(prefix);
            }
            case 3 -> {
                String period = ask("Enter a period (YYYY-MM): ");
                if (period == null) return false;
                handleTopics(period);
            }
            case 4 -> {
                String topic = ask("Enter a topic word: ");
                String start = topic == null ? null : ask("Enter the start period (YYYY-MM): ");
                String end = start == null ? null : ask("Enter the end period (YYYY-MM): ");
                if (end == null) return false;
                handleTrends(topic + " " + start + " " + end);
            }
            case 5 -> {
                String start = ask("Enter the start date (YYYY-MM-DD): ");
                String end = start == null ? null : ask("Enter the end date (YYYY-MM-DD): ");
                if (end == null) return false;
                handleArticles(start + " " + end);
            }
            case 6 -> {
                String id = ask("Enter the article ID: ");
                if (id == null) return false;
                handleArticle(id);
            }
            case 7 -> handleStats();
            default -> { }
        }
        return true;
    }

    /** Prints a prompt and returns the trimmed answer, or null if input ended. */
    private String ask(String prompt) {
        out.print(prompt);
        out.flush();
        if (!in.hasNextLine()) {
            return null;
        }
        String answer = in.nextLine().trim();
        log.info("Interactive input: \"" + answer + "\"");
        return answer;
    }

    /** stats: article count and the range of publication dates. */
    private void handleStats() {
        DatabaseStats stats = service.getStatistics();
        log.info("Statistics requested: " + stats.totalArticles() + " article(s)");
        out.println("Database statistics:");
        out.println("Total articles: " + stats.totalArticles());
        out.println("Earliest article date: " + stats.earliest().map(Object::toString).orElse("N/A"));
        out.println("Latest article date: " + stats.latest().map(Object::toString).orElse("N/A"));
    }

    /**
     * cmmand screen with the "> " prompt. 
     * 'menu' returns to the main menu and
     * 'help' lists commands. The data commands are recognised but not yet
     * implemented; anything else is reported as an unknown command.
     */
    private void commandMode() {
        out.println(LINE);
        out.println("COMMAND MODE");
        out.println(LINE);
        out.println("Enter commands directly. Type 'help' for available commands.");
        out.println("Type 'menu' to return to the main menu.");
        out.println();
        while (true) {
            out.print("> ");
            out.flush();
            if (!in.hasNextLine()) {
                return;
            }
            String line = in.nextLine().trim();
            if (line.isEmpty()) {
                continue; // ignore blank lines and just re-show the prompt
            }
            // first word is the command name; matching is case-insensitive
            String cmd = line.split("\\s+")[0].toLowerCase();
            log.info("User command: " + line);
            switch (cmd) {
                case "menu" -> {
                    return;
                }
                case "help" -> showCommandHelp();
                case "search" -> handleSearch(argsOf(line));
                case "autocomplete" -> handleAutocomplete(argsOf(line));
                case "articles" -> handleArticles(argsOf(line));
                case "article" -> handleArticle(argsOf(line));
                case "topics" -> handleTopics(argsOf(line));
                case "trends" -> handleTrends(argsOf(line));
                case "stats" -> handleStats();
                default -> out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }

    // returns everything after the command name
    private static String argsOf(String line) {
        String[] parts = line.split("\\s+", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }

    // search <keyword(s)>: prints matching article titles, one per line
    private void handleSearch(String query) {
        if (query.isEmpty()) {
            out.println("Usage: search <keyword>");
            return;
        }
        log.info("Search query: \"" + query + "\"");
        List<Article> results = service.search(query);
        log.info("Search returned " + results.size() + " result(s)");
        out.println("Search results for '" + query + "':");
        if (results.isEmpty()) {
            out.println("No articles found.");
            return;
        }
        for (Article a : results) {
            out.println(a.getTitle());
        }
    }

    // autocomplete for prefix --  prints up to 10 suggested words, one per line
    private void handleAutocomplete(String prefix) {
        if (prefix.isEmpty()) {
            out.println("Usage: autocomplete <prefix>");
            return;
        }
        // log the raw user input, then call the service and log how many suggestions it returned
        log.info("Autocomplete prefix: \"" + prefix + "\"");
        List<String> suggestions = service.autocomplete(prefix);
        log.info("Autocomplete returned " + suggestions.size() + " suggestion(s)");
        out.println("Autocomplete suggestions for '" + prefix + "':");
        
        // if no suggestions, print the required message and return
        if (suggestions.isEmpty()) {
            out.println("No suggestions found.");
            return;
        }
        for (String w : suggestions) {
            out.println(w);
        }
    }

    /** articles <start_date> <end_date>: titles published in the range, oldest first. */
    private void handleArticles(String args) {
        String[] parts = args.isEmpty() ? new String[0] : args.split("\\s+");
        if (parts.length != 2) {
            out.println("Usage: articles <start_date> <end_date>");
            return;
        }
        try {
            List<Article> results = service.getArticlesByDateRange(parts[0], parts[1]);
            log.info("Articles from " + parts[0] + " to " + parts[1] + ": " + results.size() + " result(s)");
            out.println("Articles from " + parts[0] + " to " + parts[1] + ":");
            if (results.isEmpty()) {
                out.println("No articles found.");
                return;
            }
            for (Article a : results) {
                out.println(a.getTitle());
            }
        } catch (InvalidInputException e) {
            log.warn("Invalid articles command \"" + args + "\": " + e.getMessage());
            out.println(e.getMessage());
        }
    }

    /** article <id>: prints the details of one article. */
    private void handleArticle(String id) {
        if (id.isEmpty()) {
            out.println("Usage: article <id>");
            return;
        }
        Optional<Article> found = service.getArticleById(id);
        if (found.isEmpty()) {
            log.warn("Article not found: " + id);
            out.println("Article not found: " + id);
            return;
        }
        Article a = found.get();
        log.info("Article lookup: " + id);
        out.println("Article details:");
        out.println("ID: " + a.getId());
        out.println("Date: " + a.getDate());
        out.println("Title: " + a.getTitle());
        if (a.getSource() != null) {
            out.println("Source: " + a.getSource());
        }
        if (a.getUrl() != null) {
            out.println("URL: " + a.getUrl());
        }
        out.println("Body: " + a.getBody());
    }

    /** topics <period>: the top trending words for one month. */
    private void handleTopics(String args) {
        if (args.isEmpty() || args.split("\\s+").length != 1) {
            out.println("Usage: topics <period>");
            return;
        }
        try {
            List<TopicCount> top = service.getTopTopics(args);
            log.info("Top topics for " + args + ": " + top.size() + " word(s)");
            out.println("Top topics for " + args + ":");
            if (top.isEmpty()) {
                out.println("No topics found.");
                return;
            }
            int rank = 1;
            for (TopicCount t : top) {
                out.println(rank++ + ". " + t.word() + " (" + t.count() + ")");
            }
        } catch (InvalidInputException e) {
            log.warn("Invalid topics command \"" + args + "\": " + e.getMessage());
            out.println(e.getMessage());
        }
    }

    /** trends <topic> <start> <end>: monthly counts for one word. */
    private void handleTrends(String args) {
        String[] parts = args.isEmpty() ? new String[0] : args.split("\\s+");
        if (parts.length != 3) {
            out.println("Usage: trends <topic> <start> <end>");
            return;
        }
        try {
            SortedMap<YearMonth, Integer> trend = service.getTopicTrend(parts[0], parts[1], parts[2]);
            log.info("Trend for '" + parts[0] + "' " + parts[1] + " to " + parts[2]
                    + ": " + trend.size() + " month(s)");
            out.println("Trend for topic '" + parts[0] + "' from " + parts[1] + " to " + parts[2] + ":");
            for (Map.Entry<YearMonth, Integer> e : trend.entrySet()) {
                out.println(e.getKey() + ": " + e.getValue());
            }
        } catch (InvalidInputException e) {
            log.warn("Invalid trends command \"" + args + "\": " + e.getMessage());
            out.println(e.getMessage());
        }
    }

    // command reference printed by the 'help' command in Command Mode 
    private void showCommandHelp() {
        out.println("Available commands:");
        out.println("  search <keyword>                  Search articles by keyword");
        out.println("  autocomplete <prefix>             Get autocomplete suggestions");
        out.println("  topics <period>                   Top topics for a month (YYYY-MM)");
        out.println("  trends <topic> <start> <end>      Topic trend (YYYY-MM)");
        out.println("  articles <start_date> <end_date>  Articles in a date range (YYYY-MM-DD)");
        out.println("  article <id>                      Show one article by ID");
        out.println("  stats                             Show database statistics");
        out.println("  help                              Show this command reference");
        out.println("  menu                              Return to the main menu");
    }

    // help & documentation screen reached from main menu option 3 
    private void showHelp() {
        out.println(LINE);
        out.println("HELP & DOCUMENTATION");
        out.println(LINE);
        out.println("Interactive Mode: a guided menu for searching articles and getting autocomplete suggestions.");
        out.println("Command Mode: type commands directly, e.g. 'search <keyword>' or 'autocomplete <prefix>'.");
        out.println("Available services: search articles, autocomplete, topics, trends, articles by date, stats.");
        out.println("Type 'help' inside Command Mode for the full command list.");
        out.println();
    }

    // logs the exit and prints the required thank-you and goodbye messages
    private void showExit() {
        log.info("Application exiting");
        out.println("Thank you for using the Tech News Search Engine!");
        out.println("Goodbye!");
    }
}
