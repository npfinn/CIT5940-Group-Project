package edu.upenn.cit5940.datamanagement;

import java.io.IOException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.upenn.cit5940.common.dto.Article;

public class MonthlyWordCounter {
	
	private TreeMap<YearMonth, Map<String, Integer>> wordCountsByMonth;
    private Set<String> stopWords;
    
    //create word counter to store monthly word counts
    public MonthlyWordCounter(Set<String> stopWords) {
    	
    	wordCountsByMonth = new TreeMap<>();
    	this.stopWords = stopWords;
    	
    }
    
    //counts words in article titles and bodies 
    public void buildWordCounts(List<Article> articles) {

        //Start fresh each time
        wordCountsByMonth.clear();

        if (articles == null) {
            return;
        }

        //Go through every article
        for (Article currentArticle : articles) {

            if (currentArticle == null || currentArticle.getDate() == null) {
                continue;
            }

            //Figure out which month this article belongs to
            YearMonth articleMonth = YearMonth.from(currentArticle.getDate());

            //Make an empty word counter if this month is new
            if (!wordCountsByMonth.containsKey(articleMonth)) {
                Map<String, Integer> newMonthCounts = new HashMap<>();
                wordCountsByMonth.put(articleMonth, newMonthCounts);
            }

            //Get the counter for this month
            Map<String, Integer> countsForThisMonth = wordCountsByMonth.get(articleMonth);

            //Put the article's title and body together
            String articleText = "";

            if (currentArticle.getTitle() != null) {
                articleText = articleText + currentArticle.getTitle();
            }

            if (currentArticle.getBody() != null) {
                articleText = articleText + " " + currentArticle.getBody();
            }

            //Make words lowercase and separate them
            articleText = articleText.toLowerCase();
            String[] wordsInArticle = articleText.split("[^a-z0-9]+");

            //Count every occurrence of each word
            for (String currentWord : wordsInArticle) {

                //Skip empty words and single letters
                if (currentWord.length() <= 1) {
                    continue;
                }

                //Don't count common stop words
                if (stopWords.contains(currentWord)) {
                    continue;
                }

                //First time seeing this word in the month
                if (!countsForThisMonth.containsKey(currentWord)) {
                    countsForThisMonth.put(currentWord, 0);
                }

                //Increase this word's count by one
                int previousCount = countsForThisMonth.get(currentWord);
                int updatedCount = previousCount + 1;

                countsForThisMonth.put(currentWord, updatedCount);
            }
        }
    }
    
    //get all words counts for a month
    public Map<String, Integer> getWordFrequencies(YearMonth requestedMonth){
    	
    	Map<String, Integer> results = new HashMap<>();
    	
    	if (requestedMonth == null) {
    		
    		return results;
    	}
    	
    	if (!wordCountsByMonth.containsKey(requestedMonth)) {
    		return results;
    	}
    	
    	//copy saved counts as results
    	Map<String, Integer> savedCounts = wordCountsByMonth.get(requestedMonth);
    	
    	for(String currentWord : savedCounts.keySet()) {
    		int wordCount = savedCounts.get(currentWord);
    		results.put(currentWord,  wordCount);
    	}
    	
    	return results;
    }
    
    //get word counmts for every month in the range of dates
    public SortedMap<YearMonth, Integer> getTopicTrend(String topic, YearMonth startMonth, YearMonth endMonth) {
    	
    	SortedMap<YearMonth, Integer> monthlyResults = new TreeMap<>();
    	
    	if (topic == null || startMonth == null || endMonth == null) {
    		return monthlyResults;
    	}
    	
    	if (startMonth.isAfter(endMonth)) {
    		return monthlyResults;
    	}
    	
    	//check that lowercase words match the topic
    	String wordToTrack = topic.toLowerCase();
    	
    	//start @ first month
    	YearMonth monthBeingChecked = startMonth;
    	
    	//go through months until we pass ending month
    	while (!monthBeingChecked.isAfter(endMonth)) {
    		int timesWordAppeared = 0;
    		
    		//check if there is data for this month
    		if (wordCountsByMonth.containsKey(monthBeingChecked)) {
    			
    			Map<String, Integer> countsForThisMonth = wordCountsByMonth.get(monthBeingChecked);
    			
    			//check if topic appears at given month
    			if (countsForThisMonth.containsKey(wordToTrack)) {
    				timesWordAppeared = countsForThisMonth.get(wordToTrack);
    			}
    		}
    		
    		//save count
    		monthlyResults.put(monthBeingChecked, timesWordAppeared);
    		
    		//iterate the month
    		monthBeingChecked = monthBeingChecked.plusMonths(1);
    	}
    	
    	return monthlyResults;
    }
    
    //Temporary main method for testing
    public static void main(String[] args) throws IOException {

        ArticleLoader articleLoader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        List<Article> articles = articleLoader.load("data/articles_small.csv");
        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        MonthlyWordCounter counter = new MonthlyWordCounter(stopWords);
        counter.buildWordCounts(articles);

        //Use the month from our first article
        YearMonth testMonth = YearMonth.from(articles.get(0).getDate());

        Map<String, Integer> countsForTestMonth = counter.getWordFrequencies(testMonth);

        System.out.println("Articles loaded: " + articles.size());
        System.out.println("Month being tested: " + testMonth);
        System.out.println("Twitter count: " + countsForTestMonth.get("twitter"));
        System.out.println("Stop word excluded: " + !countsForTestMonth.containsKey("the"));

        //Try tracking Twitter across three months
        YearMonth finalMonth = testMonth.plusMonths(2);

        SortedMap<YearMonth, Integer> twitterTrend = counter.getTopicTrend("TWITTER", testMonth, finalMonth);

        System.out.println("Months in trend: " + twitterTrend.size());
        System.out.println("Twitter trend: " + twitterTrend);

        //Try searching for a month with no articles
        YearMonth emptyMonth = YearMonth.of(1900, 1);
        Map<String, Integer> emptyCounts = counter.getWordFrequencies(emptyMonth);

        System.out.println("Missing month is empty: " + emptyCounts.isEmpty());
    }

}
