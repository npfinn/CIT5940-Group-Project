package edu.upenn.cit5940.processor;

import edu.upenn.cit5940.common.dto.Article;
import edu.upenn.cit5940.datamanagement.ArticleRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.SortedMap;

/**
 * Application/Logic tier entry point used by the UI. The repository is
 * injected by Main; the UI never touches the data tier directly.
 */
public class SearchEngineService {

    // max num of autocomplete suggestions shown (per the spec)
    public static final int MAX_SUGGESTIONS = 10;

    /** How many trending words the topics command shows (per the spec). */
    public static final int TOP_TOPICS = 10;

    private final ArticleRepository repository;

    public SearchEngineService(ArticleRepository repository) {
        this.repository = repository;
    }

    /** @return number of articles successfully loaded */
    public int getArticleCount() {
        return repository.getTotalArticleCount();
    }

    /**
     * finds articles containing all of the given keywords (AND, case-insensitive).
     *
     * @param query one or more keywords separated by whitespace, as typed
     * @return matching articles in load order; empty if none match or the query is blank
     */
    public List<Article> search(String query) {
        List<String> keywords = splitWords(query);
        if (keywords.isEmpty()) { // blank or null query returns empty list
            return new ArrayList<>();
        }
        return repository.searchByKeywords(keywords);
    }

    /**
     * suggests up to max suggested words starting with the prefix
     *
     * @param prefix the typed prefix; only its first word is used
     * @return suggestions in the repository's order; empty if none or the prefix is blank
     */
    public List<String> autocomplete(String prefix) {
        List<String> words = splitWords(prefix);
        if (words.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> suggestions = repository.autocomplete(words.get(0), MAX_SUGGESTIONS);
        // enforce the cap here too so the rule holds whatever the repository returns
        return suggestions.size() > MAX_SUGGESTIONS
                ? new ArrayList<>(suggestions.subList(0, MAX_SUGGESTIONS))
                : suggestions;
    }

    /**
     * Finds articles published between two dates, inclusive.
     *
     * @param startText start date typed by the user (YYYY-MM-DD)
     * @param endText   end date typed by the user (YYYY-MM-DD)
     * @return articles sorted oldest first; empty if none
     * @throws InvalidInputException if a date is malformed or not a real date,
     *                               or the start is after the end
     */
    public List<Article> getArticlesByDateRange(String startText, String endText)
            throws InvalidInputException {
        LocalDate start = InputValidator.parseDate(startText);
        LocalDate end = InputValidator.parseDate(endText);
        InputValidator.requireDateOrder(start, end);
        return repository.getArticlesByDateRange(start, end);
    }

    /**
     * Looks up one article by its unique ID.
     *
     * @param id the ID as typed; surrounding whitespace is ignored
     * @return the article, or empty if there is none (or the id is blank)
     */
    public Optional<Article> getArticleById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.getArticleById(id.trim());
    }

    /**
     * finds the top trending words for one month (titles and bodies, no stop words
     *
     * uses a min-heap capped at top topic entries: each word is pushed,
     * and when the heap grows past the cap the weakest entry (smallest count) is
     * dropped, so only the best candidates are kept; O(n log k) for n
     * distinct words, instead of sorting all n
     *
     * @param periodText month typed by the user (YYYY-MM)
     * @return up to 10 words, highest count first; ties broken alphabetically
     * @throws InvalidInputException if the period is malformed or not a real month
     */
    public List<TopicCount> getTopTopics(String periodText) throws InvalidInputException {
        YearMonth period = InputValidator.parsePeriod(periodText);
        Map<String, Integer> frequencies = repository.getWordFrequencies(period);

        // Order "weaker" first: lower count, then (for equal counts) later in the alphabet.
        // The heap root is then always the entry to evict.
        Comparator<TopicCount> weakestFirst = Comparator
                .comparingInt(TopicCount::count)
                .thenComparing(TopicCount::word, Comparator.reverseOrder());
        PriorityQueue<TopicCount> heap = new PriorityQueue<>(TOP_TOPICS + 1, weakestFirst);
        for (Map.Entry<String, Integer> e : frequencies.entrySet()) {
            heap.add(new TopicCount(e.getKey(), e.getValue()));
            if (heap.size() > TOP_TOPICS) {
                heap.poll();
            }
        }

        // Polling gives weakest to strongest, so reverse for best-first output.
        List<TopicCount> result = new ArrayList<>(heap.size());
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * monthly frequency of one topic word across an inclusive range of months
     *
     * @param topic     a single word, as typed
     * @param startText first month (YYYY-MM)
     * @param endText   last month (YYYY-MM)
     * @return one entry per month from start to end, oldest first
     * @throws InvalidInputException if a period is invalid or start is after end
     */
    public SortedMap<YearMonth, Integer> getTopicTrend(String topic, String startText, String endText)
            throws InvalidInputException {
        YearMonth start = InputValidator.parsePeriod(startText);
        YearMonth end = InputValidator.parsePeriod(endText);
        InputValidator.requirePeriodOrder(start, end);
        return repository.getTopicTrend(topic.trim(), start, end);
    }

    /** @return total article count and the earliest and latest publication dates */
    public DatabaseStats getStatistics() {
        return new DatabaseStats(repository.getTotalArticleCount(),
                repository.getEarliestDate(), repository.getLatestDate());
    }

    // splits raw user input into non-empty words
    private static List<String> splitWords(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) { // null is treated the same as blank
            return words;
        }
        // split on whitespace, trim, and ignore empty words
        for (String w : text.trim().split("\\s+")) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }
        return words;
    }
}
