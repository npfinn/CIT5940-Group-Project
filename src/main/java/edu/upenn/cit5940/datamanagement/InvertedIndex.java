package edu.upenn.cit5940.datamanagement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.upenn.cit5940.common.dto.Article;


public class InvertedIndex {
	
	 private Map<String, List<Article>> articlesForEachWord;
	    private Set<String> stopWords;
    
    //create our inverted index
    public InvertedIndex(Set<String> stopWords) {

        articlesForEachWord = new HashMap<>();
        this.stopWords = stopWords;
    }
    
    
    //construct index with article titles
    public void buildIndex(List<Article> articles) {

        //Clear the old index before building a new one
        articlesForEachWord.clear();

        //Go through each article
        for (Article currentArticle : articles) {

            //Keep track of words already added from this article
            Set<String> wordsAlreadyAdded = new HashSet<>();

            //Put the title and body together
            String articleText = currentArticle.getTitle() + " " + currentArticle.getBody();

            //Make everything lowercase
            articleText = articleText.toLowerCase();

            //Split the text into separate words
            String[] wordsInArticle = articleText.split("[^a-z0-9]+");

            //Go through all the words in this article
            for (String currentWord : wordsInArticle) {

                //Skip empty words and single letters
                if (currentWord.length() <= 1) {
                    continue;
                }

                //Skip common words like the and and
                if (stopWords.contains(currentWord)) {
                    continue;
                }

                //Don't add this article twice for the same word
                if (wordsAlreadyAdded.contains(currentWord)) {
                    continue;
                }

                //Remember that we already used this word
                wordsAlreadyAdded.add(currentWord);

                //If the word is new, make an empty list for it
                if (!articlesForEachWord.containsKey(currentWord)) {

                    List<Article> articlesWithThisWord = new ArrayList<>();

                    articlesForEachWord.put(currentWord, articlesWithThisWord);
                }

                //Get the list for this word
                List<Article> articlesWithThisWord = articlesForEachWord.get(currentWord);

                //Add the current article to that list
                articlesWithThisWord.add(currentArticle);
            }
        }
    }
    
    //find articles that contain the words we search for
    public List<Article> searchByKeywords(List<String> keywords) {

        List<Article> searchResults = new ArrayList<>();

        //Nothing to search for
        if (keywords == null || keywords.isEmpty()) {
            return searchResults;
        }

        //Store the search words after cleaning them up
        List<String> cleanedSearchWords = new ArrayList<>();

        //Go through everything the user searched for
        for (String searchText : keywords) {

            if (searchText == null) {
                return searchResults;
            }

            //Make the search lowercase
            searchText = searchText.toLowerCase();

            //Split up searches that have multiple words
            String[] separateWords = searchText.split("[^a-z0-9]+");

            for (String currentWord : separateWords) {

                //Ignore empty pieces
                if (currentWord.isEmpty()) {
                    continue;
                }

                //We don't index single letters
                if (currentWord.length() <= 1) {
                    return searchResults;
                }

                //Stop words aren't searchable
                if (stopWords.contains(currentWord)) {
                    return searchResults;
                }

                //No articles have this word
                if (!articlesForEachWord.containsKey(currentWord)) {
                    return searchResults;
                }

                //Save this word for the actual search
                cleanedSearchWords.add(currentWord);
            }
        }

        //Make sure we have at least one word to search
        if (cleanedSearchWords.isEmpty()) {
            return searchResults;
        }

        //Start with the articles containing the first word
        String firstSearchWord = cleanedSearchWords.get(0);

        List<Article> firstWordArticles = articlesForEachWord.get(firstSearchWord);

        Set<Article> articlesStillMatching = new HashSet<>(firstWordArticles);

        //Check the rest of the search words
        for (int i = 1; i < cleanedSearchWords.size(); i++) {

            String currentSearchWord = cleanedSearchWords.get(i);

            List<Article> articlesWithThisWord = articlesForEachWord.get(currentSearchWord);

            Set<Article> articlesMatchingThisWord = new HashSet<>();

            //Only keep articles that matched the earlier words too
            for (Article currentArticle : articlesWithThisWord) {

                if (articlesStillMatching.contains(currentArticle)) {

                    articlesMatchingThisWord.add(currentArticle);
                }
            }

            //Update which articles are still matching
            articlesStillMatching = articlesMatchingThisWord;
        }

        //Put the results back into their original article order
        for (Article currentArticle : firstWordArticles) {

            if (articlesStillMatching.contains(currentArticle)) {

                searchResults.add(currentArticle);
            }
        }

        return searchResults;
    }

    //Temporary main method so we can test it ourselves
    public static void main(String[] args) throws IOException {

        ArticleLoader articleLoader = new ArticleLoader();
        StopWordLoader stopWordLoader = new StopWordLoader();

        //Load our small sample files
        List<Article> articles = articleLoader.load("data/articles_small.csv");

        Set<String> stopWords = stopWordLoader.load("data/stop_words.txt");

        //Create and build our index
        InvertedIndex index = new InvertedIndex(stopWords);

        index.buildIndex(articles);

        //Try searching for two words
        List<String> wordsToSearch = new ArrayList<>();

        wordsToSearch.add("TWITTER");
        wordsToSearch.add("MUSK");

        List<Article> matchingArticles = index.searchByKeywords(wordsToSearch);

        System.out.println("Articles loaded: " + articles.size());
        System.out.println("Twitter and Musk matches: " + matchingArticles.size());
        System.out.println("First article found: " + matchingArticles.contains(articles.get(0)));

        //Now try searching for a stop word
        wordsToSearch.clear();

        wordsToSearch.add("the");

        matchingArticles = index.searchByKeywords(wordsToSearch);

        System.out.println("Stop word matches: " + matchingArticles.size());
        
        
      //Try searching for a word that doesn't exist
        wordsToSearch.clear();
        wordsToSearch.add("nonexistentwordxyz");

        matchingArticles = index.searchByKeywords(wordsToSearch);

        System.out.println("Unknown word matches: " + matchingArticles.size());

        //Try searching for the same word twice
        wordsToSearch.clear();
        wordsToSearch.add("twitter");

        matchingArticles = index.searchByKeywords(wordsToSearch);
        int singleWordCount = matchingArticles.size();

        wordsToSearch.add("twitter");

        matchingArticles = index.searchByKeywords(wordsToSearch);

        System.out.println("Repeated word gives same count: " + (singleWordCount == matchingArticles.size()));

        //Try searching for a single letter
        wordsToSearch.clear();
        wordsToSearch.add("a");

        matchingArticles = index.searchByKeywords(wordsToSearch);

        System.out.println("Single letter matches: " + matchingArticles.size());
    }
}
