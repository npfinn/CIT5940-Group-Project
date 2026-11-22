package edu.upenn.cit5940.processor;

import edu.upenn.cit5940.datamanagement.ArticleRepository;

/**
 * Application/Logic tier entry point used by the UI
 * the UI never touches the data tier directly
 */
public class SearchEngineService {

    private final ArticleRepository repository;

    public SearchEngineService(ArticleRepository repository) {
        this.repository = repository;
    }

    /** @return number of articles successfully loaded */
    public int getArticleCount() {
        return repository.getTotalArticleCount();
    }
}
