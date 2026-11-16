package edu.upenn.cit5940.datamanagement;

import edu.upenn.cit5940.common.dto.Article;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * FAKE VERSION OF ARTICLEREPOSITORYF OR TESTING PURPOSES ONLY
 * implements the same interface but skips the real work
 * 
 *
 * holds 5 hard-coded articles in a list and answers every query with
 * simple scans, so the processor and UI tiers can be built and tested
 * before the real parsers, HashMap index, Trie and TreeMap exist
 * It is a throwaway skeleton: no stop-word filtering, no indexing.
 */
public class StubArticleRepository implements ArticleRepository {

    private final List<Article> articles = new ArrayList<>();

    
}
