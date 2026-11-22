package edu.upenn.cit5940.ui;

import edu.upenn.cit5940.logging.AppLogger;
import edu.upenn.cit5940.processor.SearchEngineService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

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
    private static final String MSG_INVALID_CHOICE = "Invalid choice. Please enter 1-4: ";
    private static final String MSG_EMPTY_INPUT = "Please enter a choice (1-4): ";
    private static final String MSG_NOT_A_NUMBER = "Please enter a valid number (1-4): ";

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
            Integer choice = readMenuChoice();
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
    private Integer readMenuChoice() {
        while (true) {
            if (!in.hasNextLine()) {
                return null;
            }
            String line = in.nextLine().trim();
            String error = validateMenuInput(line);
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
    private String validateMenuInput(String line) {
        if (line.isEmpty()) {
            log.warn("Empty menu input");
            return MSG_EMPTY_INPUT;
        }
        // whole numbers (including negatives and ones too large for an int)
        // count as numeric; anything else like abc" or "1.5", does not.
        if (!line.matches("[+-]?\\d+")) {
            log.warn("Non-numeric menu input: \"" + line + "\"");
            return MSG_NOT_A_NUMBER;
        }
        if (line.matches("[+]?[1-4]")) {
            return null;
        }
        log.warn("Invalid menu choice: " + line);
        return MSG_INVALID_CHOICE;
    }

    /**
     * Guided search screen. Currently a placeholder: shows the spec-required
     * header and services, and loops until the user types 'back'.
     */
    private void interactiveMode() {
        out.println(LINE);
        out.println("INTERACTIVE MODE");
        out.println(LINE);
        out.println("Available services:");
        out.println("  - Search articles by keyword");
        out.println("  - Autocomplete suggestions");
        out.println("Enter a search keyword to see search results, or type 'back' to return to the main menu.");
        while (true) {
            out.print("Enter search keyword: ");
            out.flush();
            if (!in.hasNextLine()) {
                return;
            }
            String line = in.nextLine().trim();
            if (line.equalsIgnoreCase("back")) {
                return;
            }
            if (line.isEmpty()) {
                out.println("Please enter a search keyword.");
            } else {
                log.info("Interactive input: \"" + line + "\"");
                out.println("Search results are not available yet.");
            }
        }
    }

    /**
     * Command screen with the "> " prompt. 'menu' returns to the main menu and
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
                case "search" -> {
                    if (line.split("\\s+").length < 2) {
                        out.println("Usage: search <keyword>");
                    } else {
                        out.println("The 'search' command is not available yet.");
                    }
                }
                case "autocomplete", "topics", "trends", "articles", "article", "stats" ->
                        out.println("The '" + cmd + "' command is not available yet.");
                default -> out.println("Unknown command. Type 'help' for available commands.");
            }
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
