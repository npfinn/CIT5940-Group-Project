package edu.upenn.cit5940.common.dto;

import java.time.LocalDate;

/**
 * Immutable article record shared by all tiers.
 *
 * Keeps only the fields the program needs from articles_*.csv / .json:
 * uri (stored as id), date, title, body, url and source. The other
 * columns in the data files are ignored by the parsers.
 */
public class Article {
    private final String id;      // the "uri" field, unique per article
    private final LocalDate date;
    private final String title;
    private final String body;
    private final String url;
    private final String source;

    public Article(String id, LocalDate date, String title, String body,
                   String url, String source) {
        this.id = id;
        this.date = date;
        this.title = title;
        this.body = body;
        this.url = url;
        this.source = source;
    }

    /** Shortcut for tests and stubs that only need the searchable fields. */
    public Article(String id, LocalDate date, String title, String body) {
        this(id, date, title, body, null, null);
    }

    public String getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getUrl() { return url; }
    public String getSource() { return source; }

    @Override
    public String toString() {
        return id + " [" + date + "] " + title;
    }
}
