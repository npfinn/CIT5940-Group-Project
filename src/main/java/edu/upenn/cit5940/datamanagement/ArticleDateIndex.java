package edu.upenn.cit5940.datamanagement;


import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.upenn.cit5940.common.dto.Article;


public class ArticleDateIndex {
	
	private TreeMap<LocalDate, List<Article>> articlesByDate;
	
	//create empty treemap we can use to store the articles based on their date
	public ArticleDateIndex() {
		articlesByDate = new TreeMap<>();
	}
	
	
	//adds the articles to map using dates
	public void buildIndex(List<Article> articles) {
		
		//clear anything we already stored
		articlesByDate.clear();
		
		if (articles == null) {
			return;
		}
		
		
		//iterate through articles one by one
		for (Article currentArticle : articles) {
			
			if (currentArticle == null) {
				continue;
			}
			
			//get article publish date
			LocalDate datePublished = currentArticle.getDate();
			
			if (datePublished == null) {
				continue;
			}
			
			
			//make new list if date isnt on map
			if (!articlesByDate.containsKey(datePublished)) {
				
				List<Article> articlesFromThisDay = new ArrayList<>();
				articlesByDate.put(datePublished,  articlesFromThisDay);
			}
			
			//get list for date
			List<Article> articlesFromThisDay = articlesByDate.get(datePublished);
			
			//add article to list
			articlesFromThisDay.add(currentArticle);
			
			
		}
	}
	
	
	//Method for finding aritcles published between dates and or including both dates
	public List<Article> getArticlesByDateRange(LocalDate startDate, LocalDate endDate){
		
		List<Article> matchingArticles = new ArrayList<>();
		
		//check if both date are there
		if (startDate == null || endDate == null) {
			return matchingArticles;
		}
		
		
		//make sure start date isnt after end date
		if (startDate.isAfter(endDate)) {
			return matchingArticles;
		}
		
		//get dates we want
		Map<LocalDate, List<Article>> datesInRange = articlesByDate.subMap(startDate,  true, endDate, true);
		
		//go through dates one by one. in order
		for (LocalDate currentDate : datesInRange.keySet()) {
			
			List<Article> articlesFromThisDay = datesInRange.get(currentDate);
			
			//add articles published on date
			for (Article currentArticle : articlesFromThisDay) {
				matchingArticles.add(currentArticle);
			}
		}
		
		return matchingArticles;
		
	}
	
	//temporary method for testing
	public static void main(String[] args) throws IOException {

        ArticleLoader articleLoader = new ArticleLoader();
        List<Article> articles = articleLoader.load("data/articles_small.csv");

        ArticleDateIndex dateIndex = new ArticleDateIndex();
        dateIndex.buildIndex(articles);

        //Search for every article in our sample
        LocalDate earlyDate = LocalDate.of(1900, 1, 1);
        LocalDate lateDate = LocalDate.of(2100, 12, 31);

        List<Article> allArticles = dateIndex.getArticlesByDateRange(earlyDate, lateDate);

        System.out.println("Articles loaded: " + articles.size());
        System.out.println("Articles found in full range: " + allArticles.size());

        //Try searching the date of our first article
        LocalDate firstArticleDate = articles.get(0).getDate();

        List<Article> sameDayArticles = dateIndex.getArticlesByDateRange(firstArticleDate, firstArticleDate);

        System.out.println("First article's date: " + firstArticleDate);
        System.out.println("Articles from that date: " + sameDayArticles.size());
        System.out.println("First article found: " + sameDayArticles.contains(articles.get(0)));

        //Test dates in the wrong order
        List<Article> invalidRange = dateIndex.getArticlesByDateRange(lateDate, earlyDate);

        System.out.println("Invalid range is empty: " + invalidRange.isEmpty());

        //Check whether all dates are in order
        boolean datesAreSorted = true;
        LocalDate previousDate = null;

        for (Article currentArticle : allArticles) {

            LocalDate currentDate = currentArticle.getDate();

            if (previousDate != null && currentDate.isBefore(previousDate)) {
                datesAreSorted = false;
            }

            previousDate = currentDate;
        }

        System.out.println("Dates are sorted: " + datesAreSorted);
    }

}
