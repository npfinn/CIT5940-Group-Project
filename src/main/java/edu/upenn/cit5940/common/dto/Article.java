package edu.upenn.cit5940.common.dto;

import java.time.LocalDate;

/**
 * Immutable article record shared by all tiers.
 * Minimal version: extend with url, source, etc. as the parsers need.
 */
public class Article {
    private final String id;      // the "uri" field
    private final LocalDate date;
    private final String title;
    private final String body;

    public Article(String id, LocalDate date, String title, String body) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.body = body;
    }

    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getTitle() { return title; }
    public String getBody() { return body; }

    @Override
    public String toString() {
        return id + " [" + date + "] " + title;
    }
}
