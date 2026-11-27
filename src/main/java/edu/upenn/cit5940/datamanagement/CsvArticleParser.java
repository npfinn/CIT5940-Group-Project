package edu.upenn.cit5940.datamanagement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import edu.upenn.cit5940.common.dto.Article;
import java.io.FileReader;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import edu.upenn.cit5940.logging.AppLogger;

public class CsvArticleParser implements ArticleParser {
    
	
	@Override
	public List<Article> parse(String filePath) throws IOException {
		
		//Holds articles from CSV
		List<Article> articles = new ArrayList<>();
		
		//Add file we are reading here
		
		
		//File reading (Open the CSV)
		try (CSVReader reader = new CSVReader(new FileReader(filePath))){
			
			//find # of columns from first row
			String[] headers = reader.readNext();
			
			//Check if file is empty
			if (headers == null) {
				
				return articles;
			}
			
			
			//can use headers to find columns (Mental note to finish this after we write exception)
			//keep track of column numbers
			Map<String, Integer> columns = new HashMap<>();
			
			for (int i = 0; i < headers.length; i++) {
				
				String columnName = headers[i].trim().toLowerCase();
				columns.put(columnName, i);
			}
			
			
			//Check csv for information that we need
			if (!columns.containsKey("uri") || !columns.containsKey("date") || !columns.containsKey("title") || !columns.containsKey("body") || !columns.containsKey("url") || !columns.containsKey("source")) {
				throw new IOException("CSV does not have all the required columns");
			}
			
			
			//mental note (read article headers)
			//Read one CSV at a time and loop through till we reach end
			String[] row;
			
			while ((row = reader.readNext()) != null) {
				
				//skip rows that are incomplete (mental note: need to handle incomplete ones later)
				if (row.length <= columns.get("uri") || row.length <= columns.get("date") ||row.length <= columns.get("title") || row.length <= columns.get("body") || row.length <= columns.get("url") ||row.length <= columns.get("source")) {
					
					AppLogger.getInstance().warn("Skipping CSV record in " + filePath + ": not enough columns");
					continue;
					
				}
				
				
				//get the fields we need from each article
				String id = row[columns.get("uri")];
				String dateText = row[columns.get("date")];
				String title = row[columns.get("title")];
				String body = row[columns.get("body")];
				String url = row[columns.get("url")];
				String source = row[columns.get("source")];
				
				
				//Skip article if any info is missing
				if (id == null || id.isBlank() || dateText == null || dateText.isBlank() || title == null || title.isBlank() || body == null || body.isBlank() || url == null || url.isBlank() || source == null || source.isBlank()) {
					
					AppLogger.getInstance().warn("Skipping CSV article " + id + " in " + filePath + ": required field is blank");
					continue;
				}
				
				
				
				//try to convert*
				//convert date into localdate format
				try {
					LocalDate date = LocalDate.parse(dateText.trim());
				
				
				//make article object.
					Article article = new Article(id, date, title, body, url, source);
				
					articles.add(article);
				} catch (DateTimeParseException e) {
					
					//log bad date before skipping
					 AppLogger.getInstance().warn("Skipping CSV article " + id + " in " + filePath + ": invalid date " + dateText);
					//move to next article if we have a bad date
					continue;
				}
				
				
			}
			
			
			
		} catch (CsvValidationException e) {
			
			//check for CSV being misformed (bad format)
			throw new IOException("Can't read CSV file", e);
		}
		
		
		return articles;
		
	}
	
	//Temporary test for CSV parser (Run as independent java application. Will remove when/if we use Junit)
    public static void main(String[] args) throws IOException {

        CsvArticleParser parser = new CsvArticleParser();

        // Read our sample CSV file.
        List<Article> articles = parser.parse("data/articles_small.csv");

        // Check how many articles we loaded.
        System.out.println("Total articles: " + articles.size());

        // Check the first article to make sure the fields are correct.
        if (!articles.isEmpty()) {
            Article first = articles.get(0);
            System.out.println("First ID: " + first.getId());
            System.out.println("First date: " + first.getDate());
            System.out.println("First title: " + first.getTitle());
        }
    }

}
