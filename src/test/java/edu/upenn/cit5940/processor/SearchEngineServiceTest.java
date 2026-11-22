package edu.upenn.cit5940.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.upenn.cit5940.datamanagement.StubArticleRepository;

import org.junit.jupiter.api.Test;

class SearchEngineServiceTest {

    @Test
    void articleCountComesFromRepository() {
        SearchEngineService service = new SearchEngineService(new StubArticleRepository());
        assertEquals(5, service.getArticleCount());
    }
}
