package edu.upenn.cit5940.datamanagement;

import edu.upenn.cit5940.common.dto.Article;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/**
 * The processor package depends only on this interface, never on the
 * concrete parsers, HashMap index, Trie or TreeMap behind it.
 *

 *   - Tokenizing: one shared rule for the index, the Trie and
 *       the word counts: lower-case the text, split on any character that
 *       is not a letter or digit, then drop stop words (from
 *       stop_words.txt) and tokens of length 1. User input passed to these
 *       methods is tokenized with the same rule.
 *   - Counts: every count is a number of word occurrences, not a
 *       number of articles.
 *   - Case: every lookup is case-insensitive. Callers may pass
 *       input exactly as the user typed it.
 *   - Never null: methods return empty collections  instead of null when nothing matches.
 *   - Read-only results: callers must not modify returned
 *       collections; implementations may return unmodifiable views.
 *   - No validation of user input here: format checks (such as
 *       "2024-13" or start after end) belong to the processor tier. By
 *       the time a method is called, its arguments are already valid.
 *
 * Loading is intentionally not part of this interface. Main creates the
 * concrete repository, calls its load method once at startup, and then
 * passes it to the processor as an {@code ArticleRepository}.
 */
public interface ArticleRepository {

    // ------------------------------------------------------------------
    // search  (HashMap inverted index)
    // ------------------------------------------------------------------

    /**
     * Finds articles that contain all of the given keywords (AND).
     *
     * Matching uses the indexed keywords, so if any keyword is a stop
     * word, or is a single character, the result is empty.
     *
     * @param keywords one or more search words, raw user input
     * @return matching articles, each once, in load order; empty list if
     *         none match or if {@code keywords} is empty
     */
    List<Article> searchByKeywords(List<String> keywords);

    // ------------------------------------------------------------------
    // autocomplete  (Trie)
    // ------------------------------------------------------------------

    /**
     * Suggests full words from article titles and bodies that start with
     * the given prefix. Stop words are never suggested.
     *
     * @param prefix the typed prefix, raw user input
     * @param limit  maximum number of suggestions (the spec uses 10)
     * @return up to {@code limit} distinct words in alphabetical order;
     *         empty list if no word starts with the prefix, or if the
     *         prefix is blank or {@code limit} is 0 or less
     */
    List<String> autocomplete(String prefix, int limit);

    // ------------------------------------------------------------------
    // articles <start> <end>  (TreeMap keyed by date)
    // ------------------------------------------------------------------

    /**
     * Finds all articles published between two dates, inclusive.
     *
     * @param start first date, already validated
     * @param end   last date, already validated, not before 
     * @return articles sorted chronologically (oldest first), with
     *         articles from the same date kept in load order; empty list
     *         if none fall in the range
     */
    List<Article> getArticlesByDateRange(LocalDate start, LocalDate end);

    // ------------------------------------------------------------------
    // article <id>
    // ------------------------------------------------------------------

    /**
     * Looks up one article by its unique ID (the "uri" field).
     *
     * If the data file repeats an ID, the first article loaded keeps
     * it and the duplicate is skipped with a logged warning.
     *
     * @param id the article ID, as a string
     * @return the article, or {@link Optional#empty()} if there is none
     */
    Optional<Article> getArticleById(String id);

    // ------------------------------------------------------------------
    // topics <period>  (frequency data; the Heap lives in the processor)
    // ------------------------------------------------------------------

    /**
     * Returns the word frequencies for one month, counting words from both
     * titles and bodies, without stop words or 1-character tokens.
     *
     * The repository only supplies the counts. The processor tier puts
     * them into a Heap to pick the top 10 trending words.
     *
     * @param period the month, already validated
     * @return map of word to count for that month; empty map if no
     *         articles were published in it
     */
    Map<String, Integer> getWordFrequencies(YearMonth period);

    // ------------------------------------------------------------------
    // trends <topic> <start> <end>  (pre-stored data in a TreeMap)
    // ------------------------------------------------------------------

    /**
     * Returns how many times a topic word appears in each month of a range.
     *
     * If the topic is a stop word or a single character, every month's
     * count is 0, because those words are never counted.
     *
     * @param topic the single word to track, raw user input
     * @param start first month, already validated
     * @param end   last month, already validated, not before {@code start}
     * @return a sorted map with one entry for every month from
     *         {@code start} to {@code end} inclusive, with a count of 0
     *         for months where the topic does not appear
     */
    SortedMap<YearMonth, Integer> getTopicTrend(String topic,
                                                YearMonth start,
                                                YearMonth end);

    // ------------------------------------------------------------------
    // stats
    // ------------------------------------------------------------------

    /**
     * @return the number of articles successfully loaded
     */
    int getTotalArticleCount();

    /**
     * @return the earliest publication date, or empty if no articles
     */
    Optional<LocalDate> getEarliestDate();

    /**
     * @return the latest publication date, or empty if no articles
     */
    Optional<LocalDate> getLatestDate();
}