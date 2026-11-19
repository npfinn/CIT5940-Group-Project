package edu.upenn.cit5940.datamanagement;


import java.io.IOException;
import java.util.List;

import edu.upenn.cit5940.common.dto.Article;


public interface ArticleParser {
	
	
	//Will be easiest to just have both CSV and JSON parsers using the same method. Should read file and returns the articles that were found
	List<Article> parse(String filePath) throws IOException;
	
}
