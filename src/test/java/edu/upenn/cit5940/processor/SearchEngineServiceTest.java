package edu.upenn.cit5940.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.upenn.cit5940.common.dto.Article;
import edu.upenn.cit5940.datamanagement.ArticleRepository;
import edu.upenn.cit5940.datamanagement.StubArticleRepository;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class SearchEngineServiceTest {

    @Test
    void articleCountComesFromRepository() {
        SearchEngineService service = new SearchEngineService(new StubArticleRepository());
        assertEquals(5, service.getArticleCount());
    }

    private final SearchEngineService service = new SearchEngineService(new StubArticleRepository());

    @Test
    void searchReturnsMatchingArticles() {
        assertEquals(2, service.search("twitter").size());
        assertEquals(1, service.search("twitter musk").size());
    }

    @Test
    void searchIgnoresExtraWhitespaceAndCase() {
        assertEquals(1, service.search("  TWITTER    Musk ").size());
    }

    @Test
    void blankOrNullSearchReturnsEmpty() {
        assertTrue(service.search("").isEmpty());
        assertTrue(service.search("   ").isEmpty());
        assertTrue(service.search(null).isEmpty());
    }

    @Test
    void autocompleteBlankOrNullReturnsEmpty() {
        assertTrue(service.autocomplete("").isEmpty());
        assertTrue(service.autocomplete(null).isEmpty());
    }

    @Test
    void autocompleteUsesOnlyFirstWordAndLimitOfTen() {
        class Spy extends StubArticleRepository {
            String seenPrefix;
            int seenLimit;
            @Override
            public List<String> autocomplete(String prefix, int limit) {
                seenPrefix = prefix;
                seenLimit = limit;
                return List.of("a1", "a2");
            }
        }
        Spy spy = new Spy();
        List<String> result = new SearchEngineService(spy).autocomplete("  ab cd ");
        assertEquals("ab", spy.seenPrefix);
        assertEquals(10, spy.seenLimit);
        assertEquals(List.of("a1", "a2"), result);
    }

    @Test
    void autocompleteCapsResultsAtTenEvenIfRepositoryReturnsMore() {
        ArticleRepository greedy = new StubArticleRepository() {
            @Override
            public List<String> autocomplete(String prefix, int limit) {
                return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");
            }
        };
        assertEquals(10, new SearchEngineService(greedy).autocomplete("x").size());
    }

    @Test
    void dateRangeReturnsArticlesInclusiveAndSorted() throws Exception {
        List<Article> r = service.getArticlesByDateRange("2022-07-15", "2022-08-02");
        assertEquals(3, r.size());
        assertEquals("2", r.get(0).getId());
        assertEquals("4", r.get(2).getId());
    }

    @Test
    void dateRangeWithNoArticlesIsEmpty() throws Exception {
        assertTrue(service.getArticlesByDateRange("2000-01-01", "2000-12-31").isEmpty());
    }

    @Test
    void dateRangeRejectsBadInput() {
        assertThrows(InvalidInputException.class, () -> service.getArticlesByDateRange("2022-04-31", "2022-05-01"));
        assertThrows(InvalidInputException.class, () -> service.getArticlesByDateRange("2022-05-01", "bad"));
        assertThrows(InvalidInputException.class, () -> service.getArticlesByDateRange("2022-08-01", "2022-07-01"));
    }

    @Test
    void articleByIdFindsAndTrims() {
        assertEquals("Apple unveils new chip", service.getArticleById(" 2 ").get().getTitle());
    }

    @Test
    void articleByIdMissingOrBlankIsEmpty() {
        assertTrue(service.getArticleById("999").isEmpty());
        assertTrue(service.getArticleById("  ").isEmpty());
        assertTrue(service.getArticleById(null).isEmpty());
    }

    /** Stub with canned frequencies/trend so topics and trends have data. */
    private static class DataStub extends StubArticleRepository {
        final Map<String, Integer> freq = new HashMap<>();
        YearMonth seenPeriod;
        String seenTopic;

        @Override
        public Map<String, Integer> getWordFrequencies(YearMonth period) {
            seenPeriod = period;
            return freq;
        }

        @Override
        public SortedMap<YearMonth, Integer> getTopicTrend(String topic, YearMonth start, YearMonth end) {
            seenTopic = topic;
            SortedMap<YearMonth, Integer> m = new TreeMap<>();
            int n = 1;
            for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
                m.put(ym, n++);
            }
            return m;
        }
    }

    @Test
    void topTopicsAreHighestCountFirst() throws Exception {
        DataStub stub = new DataStub();
        stub.freq.put("low", 1);
        stub.freq.put("high", 9);
        stub.freq.put("mid", 5);
        List<TopicCount> top = new SearchEngineService(stub).getTopTopics("2022-07");
        assertEquals(List.of(new TopicCount("high", 9), new TopicCount("mid", 5), new TopicCount("low", 1)), top);
        assertEquals(YearMonth.of(2022, 7), stub.seenPeriod);
    }

    @Test
    void topTopicsKeepsOnlyTenBest() throws Exception {
        DataStub stub = new DataStub();
        for (int i = 1; i <= 25; i++) {
            stub.freq.put("w" + i, i);
        }
        List<TopicCount> top = new SearchEngineService(stub).getTopTopics("2022-07");
        assertEquals(10, top.size());
        assertEquals(new TopicCount("w25", 25), top.get(0));
        assertEquals(new TopicCount("w16", 16), top.get(9));
    }

    @Test
    void topTopicsTiesAreBrokenAlphabetically() throws Exception {
        DataStub stub = new DataStub();
        for (String w : new String[] {"delta", "alpha", "charlie", "bravo"}) {
            stub.freq.put(w, 3);
        }
        List<TopicCount> top = new SearchEngineService(stub).getTopTopics("2022-07");
        assertEquals(List.of("alpha", "bravo", "charlie", "delta"),
                top.stream().map(TopicCount::word).toList());
    }

    @Test
    void topTopicsTieAtTheCutoffKeepsAlphabeticallyFirst() throws Exception {
        DataStub stub = new DataStub();
        // 10 words tie at 5; "a"-"j" should win over "k"-"o"
        for (char c = 'a'; c <= 'o'; c++) {
            stub.freq.put(String.valueOf(c), 5);
        }
        List<TopicCount> top = new SearchEngineService(stub).getTopTopics("2022-07");
        assertEquals("a", top.get(0).word());
        assertEquals("j", top.get(9).word());
    }

    @Test
    void topTopicsWithNoDataIsEmpty() throws Exception {
        assertTrue(service.getTopTopics("2022-07").isEmpty());
    }

    @Test
    void topTopicsRejectsBadPeriod() {
        assertThrows(InvalidInputException.class, () -> service.getTopTopics("2024-13"));
        assertThrows(InvalidInputException.class, () -> service.getTopTopics("July"));
    }

    @Test
    void trendReturnsOneEntryPerMonth() throws Exception {
        DataStub stub = new DataStub();
        SortedMap<YearMonth, Integer> t = new SearchEngineService(stub).getTopicTrend(" ai ", "2022-07", "2022-09");
        assertEquals(3, t.size());
        assertEquals(YearMonth.of(2022, 7), t.firstKey());
        assertEquals("ai", stub.seenTopic);
    }

    @Test
    void trendWithSameStartAndEndIsOneMonth() throws Exception {
        assertEquals(1, service.getTopicTrend("ai", "2022-07", "2022-07").size());
    }

    @Test
    void trendRejectsBadPeriodsAndBadOrder() {
        assertThrows(InvalidInputException.class, () -> service.getTopicTrend("ai", "2022-13", "2022-14"));
        assertThrows(InvalidInputException.class, () -> service.getTopicTrend("ai", "2022-07", "x"));
        assertThrows(InvalidInputException.class, () -> service.getTopicTrend("ai", "2022-09", "2022-07"));
    }

    @Test
    void statisticsFromStub() {
        DatabaseStats s = service.getStatistics();
        assertEquals(5, s.totalArticles());
        assertEquals(java.time.LocalDate.of(2022, 7, 8), s.earliest().get());
        assertEquals(java.time.LocalDate.of(2022, 9, 20), s.latest().get());
    }

    @Test
    void statisticsWithNoArticlesHaveEmptyDates() {
        ArticleRepository empty = new StubArticleRepository() {
            @Override public int getTotalArticleCount() { return 0; }
            @Override public java.util.Optional<java.time.LocalDate> getEarliestDate() { return java.util.Optional.empty(); }
            @Override public java.util.Optional<java.time.LocalDate> getLatestDate() { return java.util.Optional.empty(); }
        };
        DatabaseStats s = new SearchEngineService(empty).getStatistics();
        assertEquals(0, s.totalArticles());
        assertTrue(s.earliest().isEmpty() && s.latest().isEmpty());
    }
}
