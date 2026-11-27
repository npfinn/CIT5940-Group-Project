package edu.upenn.cit5940.datamanagement;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;


import java.util.HashMap; //Hashmap to hold article fields
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import edu.upenn.cit5940.common.dto.Article;

import edu.upenn.cit5940.logging.AppLogger;

public class JsonArticleParser implements ArticleParser {
	
	@Override
	public List<Article> parse(String filePath) throws IOException{
		
		//Store articles from JSON
		List<Article> articles = new ArrayList<>();
		
		//Open JSON with Jackson
		JsonFactory factory = new JsonFactory();
		
		try (JsonParser parser = factory.createParser(new File(filePath))){
			
			
			//Json file should have an array at the beggining
			if (parser.nextToken() != JsonToken.START_ARRAY) {
				throw new IOException("JSON file must contain an array");
			}
			
			//can read articles here (mental note)
			while (parser.nextToken() == JsonToken.START_OBJECT) {
				
				Map<String, String> fields = new HashMap<>();
			
			//read till end of article
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				
				String fieldName = parser.currentName();
				parser.nextToken();
				
				//save value of current field
				fields.put(fieldName,  parser.getValueAsString());
				
				//skip extra data
				parser.skipChildren();
			}

			
			//get the fields we need from each article
			String id = fields.get("uri");
			String dateText = fields.get("date");
			String title = fields.get("title");
			String body = fields.get("body");
			String url = fields.get("url");
			String source = fields.get("source");
			
			
			//Skip article if any info is missing
			if (id != null && !id.isBlank() && dateText != null && !dateText.isBlank() && title != null && !title.isBlank() && body != null && !body.isBlank() && url != null && !url.isBlank() && source != null && !source.isBlank()) {
				
			
			
			
			
			//try to convert*
			//convert date into localdate format
				try {
					LocalDate date = LocalDate.parse(dateText.trim());
			
			
					//make article object.
					Article article = new Article(id, date, title, body, url, source);
			
					articles.add(article);
				} catch (DateTimeParseException e) {
					
					//Log the bad date before moving to the next article
					AppLogger.getInstance().warn("Skipping JSON article " + id + " in " + filePath + ": invalid date " + dateText);	
				}
				
				
			
			} else {
				 //Log articles with missing or blank fields
			    AppLogger.getInstance().warn("Skipping JSON article " + id + " in " + filePath + ": required field is missing or blank");
			}
			
			}
			
			return articles;
			
			}
	}
	
	//Quick test for our JSON parser.
    //Can remove this once we have JUnit tests.
    public static void main(String[] args) throws IOException {

        JsonArticleParser parser = new JsonArticleParser();

        //Read our sample JSON file.
        List<Article> articles = parser.parse("data/articles_small.json");

        //Check how many articles were loaded.
        System.out.println("Total articles: " + articles.size());

        //Check the first article.
        if (!articles.isEmpty()) {
            Article first = articles.get(0);

            System.out.println("First ID: " + first.getId());
            System.out.println("First date: " + first.getDate());
            System.out.println("First title: " + first.getTitle());
        }
        //Read the same articles from the CSV file.
        CsvArticleParser csvParser = new CsvArticleParser();
        List<Article> csvArticles = csvParser.parse("data/articles_small.csv");

        //Make sure both files gave us the same articles.
        boolean sameArticles = articles.size() == csvArticles.size();

        if (sameArticles) {
            for (int i = 0; i < articles.size(); i++) {
                Article jsonArticle = articles.get(i);
                Article csvArticle = csvArticles.get(i);

                if (!jsonArticle.getId().equals(csvArticle.getId())
                        || !jsonArticle.getDate().equals(csvArticle.getDate())
                        || !jsonArticle.getTitle().equals(csvArticle.getTitle())
                        || !jsonArticle.getBody().equals(csvArticle.getBody())
                        || !jsonArticle.getUrl().equals(csvArticle.getUrl())
                        || !jsonArticle.getSource().equals(csvArticle.getSource())) {
                    sameArticles = false;
                    break;
                }
            }
        }

        System.out.println("CSV and JSON match: " + sameArticles);
    }
}
			
			
	