package edu.upenn.cit5940.datamanagement;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import edu.upenn.cit5940.common.dto.Article;

class ArticleDataRepositoryTest {

	//Make sure our repository loads all 100 sample articles
    @Test
    public void testTotalArticleCount() throws IOException {

        //Load our sample articles and stop words
        ArticleLoader loader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        List<Article> articles = loader.load("data/articles_small.csv");
        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        //Create our repository using the loaded data
        ArticleDataRepository repository = new ArticleDataRepository(articles, stopWords);

        //Check that we loaded exactly 100 articles
        assertEquals(100, repository.getTotalArticleCount());
    }
    
    //Test finding articles using their IDs
    @Test
    public void testGetArticleById() throws IOException {

        //Load our sample data
        ArticleLoader loader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        List<Article> articles = loader.load("data/articles_small.csv");
        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        //Create our repository
        ArticleDataRepository repository = new ArticleDataRepository(articles, stopWords);

        //Make sure we can find an existing article
        assertTrue(repository.getArticleById("7108037875").isPresent());

        //Make sure a fake article ID returns nothing
        assertFalse(repository.getArticleById("NOT_REAL").isPresent());
    }
    
    //Check that searches find articles with all the words
    @Test
    public void testSearchByKeywords() throws IOException {

        ArticleLoader articleLoader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        List<Article> sampleArticles = articleLoader.load("data/articles_small.csv");
        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        ArticleDataRepository repository = new ArticleDataRepository(sampleArticles, stopWords);

        //Look for articles with both words
        List<String> wordsToFind = new ArrayList<>();
        wordsToFind.add("Twitter");
        wordsToFind.add("Musk");

        List<Article> matchingArticles = repository.searchByKeywords(wordsToFind);

        assertEquals(3, matchingArticles.size());

        //Changing capitalization should give the same articles
        wordsToFind.clear();
        wordsToFind.add("TWITTER");
        wordsToFind.add("musk");

        assertEquals(matchingArticles, repository.searchByKeywords(wordsToFind));

        //A word we dont have should return nothing
        wordsToFind.clear();
        wordsToFind.add("notarealword12345");

        assertTrue(repository.searchByKeywords(wordsToFind).isEmpty());

        //Stop words should return nothing
        wordsToFind.clear();
        wordsToFind.add("the");

        assertTrue(repository.searchByKeywords(wordsToFind).isEmpty());
    }
    //Check suggestions using words from our sample articles
    @Test
    public void testAutocomplete() throws IOException {

        ArticleLoader articleLoader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        List<Article> sampleArticles = articleLoader.load("data/articles_small.csv");
        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        ArticleDataRepository repository = new ArticleDataRepository(sampleArticles, stopWords);

        //We know Twitter appears in our sample
        List<String> suggestedWords = repository.autocomplete("twit", 10);

        assertTrue(suggestedWords.contains("twitter"));
        assertTrue(suggestedWords.size() <= 10);

        //Every suggestion should start with what we typed
        for (String suggestedWord : suggestedWords) {
            assertTrue(suggestedWord.startsWith("twit"));
        }

        //Capitalization shouldnt change the suggestions
        assertEquals(suggestedWords, repository.autocomplete("TWIT", 10));

        //Check that suggestions come back alphabetically
        List<String> sortedSuggestions = new ArrayList<>(suggestedWords);
        java.util.Collections.sort(sortedSuggestions);

        assertEquals(sortedSuggestions, suggestedWords);

        //Check smaller limits and inputs with no matches
        assertEquals(1, repository.autocomplete("twit", 1).size());
        assertTrue(repository.autocomplete("twit", 0).isEmpty());
        assertTrue(repository.autocomplete("", 10).isEmpty());
        assertTrue(repository.autocomplete("zzzznotaword", 10).isEmpty());
    }
    
  //Make sure suggestions leave out stop words and single letters
    @Test
    public void testAutocompleteFiltering() {

        List<Article> articlesForSuggestions = new ArrayList<>();
        articlesForSuggestions.add(new Article("suggestions-17", java.time.LocalDate.of(2022, 11, 8), "Chip shortage", "the chip shipment reached a warehouse"));
        Set<String> wordsToSkip = new java.util.HashSet<>();
        wordsToSkip.add("the");

        ArticleDataRepository repository = new ArticleDataRepository(articlesForSuggestions, wordsToSkip);

        //Keep useful words from both the title and body
        assertTrue(repository.autocomplete("ch", 10).contains("chip"));
        assertTrue(repository.autocomplete("sh", 10).contains("shipment"));

        //The stop word shouldnt be suggested
        assertFalse(repository.autocomplete("th", 10).contains("the"));

        //Single letters should be skipped even without being stop words
        assertFalse(repository.autocomplete("a", 10).contains("a"));
    }
    //Check date boundaries and the order articles come back in
    @Test
    public void testArticlesByDateRange() {

        List<Article> articlesForDates = new ArrayList<>();
        Article warehouseUpdate = new Article("warehouse-28", LocalDate.of(2022, 11, 18), "Warehouse sensors updated", "New sensors track shipments");
        Article chipDelivery = new Article("delivery-43", LocalDate.of(2022, 11, 6), "Chip delivery arrives", "The shipment reached the warehouse");
        Article shippingNotice = new Article("notice-12", LocalDate.of(2022, 11, 18), "Shipping schedule revised", "Deliveries will resume next week");

        //Load these out of date order
        articlesForDates.add(warehouseUpdate);
        articlesForDates.add(chipDelivery);
        articlesForDates.add(shippingNotice);

        Set<String> wordsToSkip = new java.util.HashSet<>();
        ArticleDataRepository repository = new ArticleDataRepository(articlesForDates, wordsToSkip);

        List<Article> articlesInRange = repository.getArticlesByDateRange(LocalDate.of(2022, 11, 6), LocalDate.of(2022, 11, 18));

        //Include both boundaries and put the oldest article first
        assertEquals(3, articlesInRange.size());
        assertEquals(chipDelivery, articlesInRange.get(0));

        //Articles from the same day should stay in load order
        assertEquals(warehouseUpdate, articlesInRange.get(1));
        assertEquals(shippingNotice, articlesInRange.get(2));

        //A single day should include articles published that day
        assertEquals(2, repository.getArticlesByDateRange(LocalDate.of(2022, 11, 18), LocalDate.of(2022, 11, 18)).size());

        //Dates without articles should return nothing
        assertTrue(repository.getArticlesByDateRange(LocalDate.of(2022, 12, 1), LocalDate.of(2022, 12, 31)).isEmpty());
    }
    //Check monthly counts using words from both titles and bodies
    @Test
    public void testMonthlyWordCounts() {

        List<Article> articlesForCounts = new ArrayList<>();
        articlesForCounts.add(new Article("dispatch-36", LocalDate.of(2022, 11, 9), "Warehouse dispatch", "The warehouse received a shipment shipment"));
        articlesForCounts.add(new Article("dispatch-52", LocalDate.of(2022, 11, 21), "Shipment delayed", "Warehouse dispatch resumed"));
        articlesForCounts.add(new Article("dispatch-68", LocalDate.of(2022, 12, 4), "Warehouse update", "Shipment arrived"));

        Set<String> wordsToSkip = new java.util.HashSet<>();
        wordsToSkip.add("the");

        ArticleDataRepository repository = new ArticleDataRepository(articlesForCounts, wordsToSkip);

        java.util.Map<String, Integer> novemberCounts = repository.getWordFrequencies(YearMonth.of(2022, 11));

        //Count every occurrence across both November articles
        assertEquals(3, novemberCounts.getOrDefault("warehouse", 0).intValue());
        assertEquals(3, novemberCounts.getOrDefault("shipment", 0).intValue());
        assertEquals(2, novemberCounts.getOrDefault("dispatch", 0).intValue());

        //Leave out stop words and single letters
        assertFalse(novemberCounts.containsKey("the"));
        assertFalse(novemberCounts.containsKey("a"));

        //Keep December counts separate
        java.util.Map<String, Integer> decemberCounts = repository.getWordFrequencies(YearMonth.of(2022, 12));
        assertEquals(1, decemberCounts.getOrDefault("warehouse", 0).intValue());
        assertEquals(1, decemberCounts.getOrDefault("shipment", 0).intValue());

        //A month without articles should return an empty map
        assertTrue(repository.getWordFrequencies(YearMonth.of(2023, 1)).isEmpty());
    }
    //Check topic counts across months with a gap between articles
    @Test
    public void testTopicTrends() {

        List<Article> articlesForTrends = new ArrayList<>();
        articlesForTrends.add(new Article("freight-24", LocalDate.of(2022, 11, 13), "Freight schedule", "Freight routes changed"));
        articlesForTrends.add(new Article("freight-61", LocalDate.of(2023, 1, 8), "Freight update", "The depot reopened"));

        Set<String> wordsToSkip = new java.util.HashSet<>();
        wordsToSkip.add("the");

        ArticleDataRepository repository = new ArticleDataRepository(articlesForTrends, wordsToSkip);

        java.util.SortedMap<YearMonth, Integer> freightByMonth = repository.getTopicTrend("FREIGHT", YearMonth.of(2022, 11), YearMonth.of(2023, 1));

        //Include both ending months and the empty month between them
        assertEquals(3, freightByMonth.size());
        assertEquals(2, freightByMonth.get(YearMonth.of(2022, 11)).intValue());
        assertEquals(0, freightByMonth.get(YearMonth.of(2022, 12)).intValue());
        assertEquals(1, freightByMonth.get(YearMonth.of(2023, 1)).intValue());

        //Keep the months in chronological order
        assertEquals(YearMonth.of(2022, 11), freightByMonth.firstKey());
        assertEquals(YearMonth.of(2023, 1), freightByMonth.lastKey());

        //A single-month range should return one entry
        java.util.SortedMap<YearMonth, Integer> januaryCount = repository.getTopicTrend("freight", YearMonth.of(2023, 1), YearMonth.of(2023, 1));
        assertEquals(1, januaryCount.size());
        assertEquals(1, januaryCount.get(YearMonth.of(2023, 1)).intValue());

        //Stop words should have zero occurrences
        java.util.SortedMap<YearMonth, Integer> skippedWordCounts = repository.getTopicTrend("the", YearMonth.of(2022, 11), YearMonth.of(2023, 1));

        for (Integer monthlyCount : skippedWordCounts.values()) {
            assertEquals(0, monthlyCount.intValue());
        }
    }
  //Check that an empty repository returns empty results
    @Test
    public void testEmptyRepository() {

        List<Article> noArticles = new ArrayList<>();
        Set<String> wordsToSkip = new java.util.HashSet<>();

        ArticleDataRepository repository = new ArticleDataRepository(noArticles, wordsToSkip);

        assertEquals(0, repository.getTotalArticleCount());
        assertTrue(repository.getEarliestDate().isEmpty());
        assertTrue(repository.getLatestDate().isEmpty());
        assertTrue(repository.getArticleById("depot-39").isEmpty());

        List<String> wordsToFind = new ArrayList<>();
        wordsToFind.add("depot");

        assertTrue(repository.searchByKeywords(wordsToFind).isEmpty());
        assertTrue(repository.autocomplete("dep", 10).isEmpty());
        assertTrue(repository.getArticlesByDateRange(LocalDate.of(2022, 11, 1), LocalDate.of(2023, 1, 31)).isEmpty());
        assertTrue(repository.getWordFrequencies(YearMonth.of(2022, 11)).isEmpty());

        //Trends should still include each month with a zero count
        java.util.SortedMap<YearMonth, Integer> emptyMonthCounts = repository.getTopicTrend("depot", YearMonth.of(2022, 11), YearMonth.of(2023, 1));

        assertEquals(3, emptyMonthCounts.size());
        assertEquals(YearMonth.of(2022, 11), emptyMonthCounts.firstKey());
        assertEquals(YearMonth.of(2023, 1), emptyMonthCounts.lastKey());

        for (Integer monthlyCount : emptyMonthCounts.values()) {
            assertEquals(0, monthlyCount.intValue());
        }
    }

    //Keep the first article when another article has the same ID
    @Test
    public void testDuplicateArticleIds() {

        Article firstDelivery = new Article("delivery-84", LocalDate.of(2022, 11, 7), "Depot delivery", "Depot received crates");
        Article repeatedDelivery = new Article("delivery-84", LocalDate.of(2022, 12, 19), "Forklift repair", "Forklift returned");

        List<Article> deliveryRecords = new ArrayList<>();
        deliveryRecords.add(firstDelivery);
        deliveryRecords.add(repeatedDelivery);

        Set<String> wordsToSkip = new java.util.HashSet<>();
        ArticleDataRepository repository = new ArticleDataRepository(deliveryRecords, wordsToSkip);

        assertEquals(1, repository.getTotalArticleCount());
        assertEquals(firstDelivery, repository.getArticleById("delivery-84").get());

        //The duplicate date shouldnt change our date range
        assertEquals(firstDelivery.getDate(), repository.getEarliestDate().get());
        assertEquals(firstDelivery.getDate(), repository.getLatestDate().get());

        List<Article> savedDeliveries = repository.getArticlesByDateRange(LocalDate.of(2022, 11, 1), LocalDate.of(2022, 12, 31));
        assertEquals(1, savedDeliveries.size());
        assertEquals(firstDelivery, savedDeliveries.get(0));

        //Words from the duplicate shouldnt enter either index
        List<String> wordsToFind = new ArrayList<>();
        wordsToFind.add("forklift");

        assertTrue(repository.searchByKeywords(wordsToFind).isEmpty());
        assertTrue(repository.autocomplete("fork", 10).isEmpty());

        wordsToFind.clear();
        wordsToFind.add("depot");

        List<Article> depotMatches = repository.searchByKeywords(wordsToFind);
        assertEquals(1, depotMatches.size());
        assertEquals(firstDelivery, depotMatches.get(0));

        //Only the first article should contribute word counts
        java.util.Map<String, Integer> novemberCounts = repository.getWordFrequencies(YearMonth.of(2022, 11));
        assertEquals(2, novemberCounts.getOrDefault("depot", 0).intValue());
        assertTrue(repository.getWordFrequencies(YearMonth.of(2022, 12)).isEmpty());

        java.util.SortedMap<YearMonth, Integer> depotByMonth = repository.getTopicTrend("depot", YearMonth.of(2022, 11), YearMonth.of(2022, 12));
        assertEquals(2, depotByMonth.get(YearMonth.of(2022, 11)).intValue());
        assertEquals(0, depotByMonth.get(YearMonth.of(2022, 12)).intValue());
    }

}
