package edu.upenn.cit5940.datamanagement;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

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

}
