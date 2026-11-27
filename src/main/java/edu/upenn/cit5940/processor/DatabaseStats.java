package edu.upenn.cit5940.processor;

import java.time.LocalDate;
import java.util.Optional;

// summary numbers shown by the stats command; dates are empty when there are no articles
public record DatabaseStats(int totalArticles, Optional<LocalDate> earliest, Optional<LocalDate> latest) { }
