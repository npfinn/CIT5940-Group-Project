package edu.upenn.cit5940.datamanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.time.YearMonth;
import java.util.SortedMap;

import java.time.LocalDate;
import java.util.Optional;
import java.io.IOException;





import edu.upenn.cit5940.common.dto.Article;


public class ArticleDataRepository implements ArticleRepository {
	
	private List<Article> loadedArticles;
	private Map<String, Article> articlesById;
	
	
	private InvertedIndex keywordIndex;
	private CustomTrie wordSuggestions;
	private ArticleDateIndex dateIndex;
	private MonthlyWordCounter monthlyCounts;
	
	//Setup repo with loaded articles
	public ArticleDataRepository(List<Article> articlesFromFile, Set<String> stopWords) {
		
		loadedArticles = new ArrayList<>();
		
		articlesById = new HashMap<>();
		
		//make sure we only keep each article once
		if (articlesFromFile != null) {
			for (Article currentArticle : articlesFromFile) {
				
				if (currentArticle == null || currentArticle.getId() == null) {
					continue;
				}
				
				String articleId = currentArticle.getId();
				
				if(!articlesById.containsKey(articleId)) {
					loadedArticles.add(currentArticle);
					articlesById.put(articleId, currentArticle);
				}
			}
		}
		
		//use empty set if we dont provide stop words
		if (stopWords == null) {
			stopWords = new HashSet<>();
		}
		
		//create search index by keywords
		keywordIndex = new InvertedIndex(stopWords);
		keywordIndex.buildIndex(loadedArticles);
		
		//build trie
		wordSuggestions = new CustomTrie();
		wordSuggestions.addArticleWords(loadedArticles);;
		
		//put articles into order based on date
		dateIndex = new ArticleDateIndex();
		dateIndex.buildIndex(loadedArticles);;
		
		//counts monthly words
		monthlyCounts = new MonthlyWordCounter(stopWords);
		monthlyCounts.buildWordCounts(loadedArticles);
		
	}
	
	//Get how many articles we actually loaded
    public int getTotalArticleCount() {
        return loadedArticles.size();
    }

    //Find a specific article using its ID
    public Optional<Article> getArticleById(String articleId) {

        //Check if we have an article with this ID
        if (articlesById.containsKey(articleId)) {
            Article foundArticle = articlesById.get(articleId);
            return Optional.of(foundArticle);
        }

        //Nothing matched this ID
        return Optional.empty();
    }

    //Find the oldest article date
    public Optional<LocalDate> getEarliestDate() {

        LocalDate oldestDateFound = null;

        //Look through all the articles
        for (Article currentArticle : loadedArticles) {

            LocalDate datePublished = currentArticle.getDate();

            //Skip articles without a date
            if (datePublished == null) {
                continue;
            }

            //Keep this date if it's the oldest so far
            if (oldestDateFound == null || datePublished.isBefore(oldestDateFound)) {
                oldestDateFound = datePublished;
            }
        }

        //Handle the case where we have no dates
        if (oldestDateFound == null) {
            return Optional.empty();
        }

        return Optional.of(oldestDateFound);
    }

    //Find the newest article date
    public Optional<LocalDate> getLatestDate() {

        LocalDate newestDateFound = null;

        //Look through all the articles
        for (Article currentArticle : loadedArticles) {

            LocalDate datePublished = currentArticle.getDate();

            //Skip articles without a date
            if (datePublished == null) {
                continue;
            }

            //Keep this date if it's the newest so far
            if (newestDateFound == null || datePublished.isAfter(newestDateFound)) {
                newestDateFound = datePublished;
            }
        }

        //Handle the case where we have no dates
        if (newestDateFound == null) {
            return Optional.empty();
        }

        return Optional.of(newestDateFound);
    }
    //Search for articles containing all the given words
    public List<Article> searchByKeywords(List<String> keywords) {

        //Let our inverted index handle the searching
        List<Article> articlesThatMatched = keywordIndex.searchByKeywords(keywords);

        return articlesThatMatched;
    }

    //Find words that start with whatever the user typed
    public List<String> autocomplete(String prefix, int limit) {

        //Let our Trie find the suggestions
        List<String> wordsThatMatch = wordSuggestions.autocomplete(prefix, limit);

        return wordsThatMatch;
    }

    //Find articles published between two dates
    public List<Article> getArticlesByDateRange(LocalDate start, LocalDate end) {

        //Let our TreeMap handle the date searching
        List<Article> articlesBetweenDates = dateIndex.getArticlesByDateRange(start, end);

        return articlesBetweenDates;
    }
    
  //Get how many times each word appeared during a month
    public Map<String, Integer> getWordFrequencies(YearMonth period) {

        //Get the saved word counts for this month
        Map<String, Integer> wordsForThisMonth = monthlyCounts.getWordFrequencies(period);

        return wordsForThisMonth;
    }

    //See how often a word appeared across different months
    public SortedMap<YearMonth, Integer> getTopicTrend(String topic, YearMonth start, YearMonth end) {

        //Get the monthly counts for the word we're tracking
        SortedMap<YearMonth, Integer> topicCounts = monthlyCounts.getTopicTrend(topic, start, end);

        return topicCounts;
    }
    
 

}
