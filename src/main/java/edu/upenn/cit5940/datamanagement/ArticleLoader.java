package edu.upenn.cit5940.datamanagement;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.upenn.cit5940.common.dto.Article;

public class ArticleLoader {
	
	//loads articles from the CSV or the JSON. returns the list of unique articles
	public List<Article> load(String filePath) throws IOException {

        ArticleParser parser;

        //Figure out which type of file we are reading
        if (filePath.toLowerCase().endsWith(".csv")) {

            parser = new CsvArticleParser();

        } else if (filePath.toLowerCase().endsWith(".json")) {

            parser = new JsonArticleParser();

        } else {

            //We only support CSV and JSON files
            throw new IllegalArgumentException("File must be CSV or JSON");
        }

        //Read all the articles using the correct parser
        List<Article> articles = parser.parse(filePath);

        //Create a list for the articles we want to keep
        List<Article> uniqueArticles = new ArrayList<>();

        //Keep track of article IDs we have already seen
        Set<String> usedIds = new HashSet<>();

        //Go through each article one at a time
        for (Article article : articles) {

            String id = article.getId();

            //Only keep an article if its ID is new
            if (!usedIds.contains(id)) {

                //Remember this ID so we don't add it again
                usedIds.add(id);

                //Add the article to our final list
                uniqueArticles.add(article);
            }
        }

        return uniqueArticles;
    }

    //Temporary main method for testing
    public static void main(String[] args) throws IOException {

        ArticleLoader loader = new ArticleLoader();

        //Test loading the CSV file
        List<Article> csvArticles = loader.load("data/articles_small.csv");

        //Test loading the JSON file
        List<Article> jsonArticles = loader.load("data/articles_small.json");

        //Print our results
        System.out.println("CSV articles: " + csvArticles.size());
        System.out.println("JSON articles: " + jsonArticles.size());

        //Check the first article
        System.out.println("First article ID: " + csvArticles.get(0).getId());
    }

}
