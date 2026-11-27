package edu.upenn.cit5940.datamanagement;

import edu.upenn.cit5940.common.dto.Article;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * FAKE VERSION OF ARTICLEREPOSITORYF OR TESTING PURPOSES ONLY
 * implements the same interface but skips the real work
 * 
 *
 * holds 5 hard-coded articles in a list and answers every query with
 * simple scans, so the processor and UI tiers can be built and tested
 * before the real parsers, HashMap index, Trie and TreeMap exist
 * It is a throwaway skeleton: no stop-word filtering, no indexing.
 */
public class StubArticleRepository implements ArticleRepository {

    private final List<Article> articles = new ArrayList<>();

    public StubArticleRepository() {
        add("1", "2022-07-08", "Twitter board vows legal action", "Twitter will sue Musk over the merger.");
        add("2", "2022-07-15", "Apple unveils new chip", "Apple announced a faster chip for laptops.");
        add("3", "2022-07-15", "Google expands cloud region", "Google cloud adds a new region.");
        add("4", "2022-08-02", "Tesla robotaxi plans delayed", "Tesla delays robotaxi plans, Musk says.");
        add("5", "2022-09-20", "Twitter trial date set", "A judge set the Twitter trial for October.");
    }

    private void add(String id, String date, String title, String body) {
        articles.add(new Article(id, LocalDate.parse(date), title, body));
    }

    @Override
    public List<Article> searchByKeywords(List<String> keywords) {
        List<Article> result = new ArrayList<>();
        if (keywords == null || keywords.isEmpty()) {
            return result;
        }
        for (Article a : articles) {
            String text = (a.getTitle() + " " + a.getBody()).toLowerCase();
            boolean all = true;
            for (String k : keywords) {
                if (!text.contains(k.toLowerCase())) {
                    all = false;
                }
            }
            if (all) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public List<String> autocomplete(String prefix, int limit) {
        // TODO real version uses a Trie
        return new ArrayList<>();
    }

    @Override
    public List<Article> getArticlesByDateRange(LocalDate start, LocalDate end) {
        List<Article> result = new ArrayList<>();
        for (Article a : articles) {
            if (!a.getDate().isBefore(start) && !a.getDate().isAfter(end)) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public Optional<Article> getArticleById(String id) {
        for (Article a : articles) {
            if (a.getId().equals(id)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Integer> getWordFrequencies(YearMonth period) {
        // TODO real version counts tokens per month
        return new HashMap<>();
    }

    @Override
    public SortedMap<YearMonth, Integer> getTopicTrend(String topic, YearMonth start, YearMonth end) {
        SortedMap<YearMonth, Integer> trend = new TreeMap<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            trend.put(m, 0);   // TODO real counts
        }
        return trend;
    }

    @Override
    public int getTotalArticleCount() {
        return articles.size();
    }

    @Override
    public Optional<LocalDate> getEarliestDate() {
        return Optional.of(LocalDate.parse("2022-07-08"));
    }

    @Override
    public Optional<LocalDate> getLatestDate() {
        return Optional.of(LocalDate.parse("2022-09-20"));
    }
}
