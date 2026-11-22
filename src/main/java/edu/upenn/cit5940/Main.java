package edu.upenn.cit5940;

import edu.upenn.cit5940.datamanagement.ArticleRepository;
import edu.upenn.cit5940.datamanagement.StubArticleRepository;
import edu.upenn.cit5940.logging.AppLogger;
import edu.upenn.cit5940.processor.SearchEngineService;
import edu.upenn.cit5940.ui.ConsoleUI;

/**
 * Program entry point and composition root.
 *
 * Per the n-tier rules, all dependencies are created here and passed down
 * through constructors: repository (data tier) -> service (processor tier)
 * -> console UI (presentation tier). No tier creates the tier below it.
 *
 * Usage: java Main [dataFile] [logFile]
 *   dataFile  optional, defaults to data/articles.csv
 *   logFile   optional, defaults to tech_news_search.log
 */
public class Main {
    // data file used when no first argument is given
    private static final String DEFAULT_DATA_FILE = "data/articles.csv";
    // log file used when no second argument is given
    private static final String DEFAULT_LOG_FILE = "tech_news_search.log";

    public static void main(String[] args) {
        // 1. Parse optional command-line arguments (data file, then log file)
        String dataPath = args.length > 0 ? args[0] : DEFAULT_DATA_FILE;
        String logPath = args.length > 1 ? args[1] : DEFAULT_LOG_FILE;

        // 2. Configure the global logger first so every later step can log
        AppLogger.init(logPath);
        AppLogger.getInstance().info("Application starting");

        // 3. Build the data tier. The real repository will parse and index dataPath here.
        // TODO: replace the stub with the real repository that loads dataPath
        ArticleRepository repository = new StubArticleRepository();
        AppLogger.getInstance().info("Loaded " + repository.getTotalArticleCount()
                + " articles from " + dataPath);

        // 4. Build the processor tier on top of the repository (dependency injection).
        SearchEngineService service = new SearchEngineService(repository);
        // 5. Build the presentation tier, print startup messages, then hand control to the menu loop.
        ConsoleUI ui = new ConsoleUI(service, System.in, System.out);
        ui.showStartup(dataPath);
        ui.run();
    }
}
