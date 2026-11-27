package edu.upenn.cit5940.processor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * validates and parses user-typed dates (YYYY-MM-DD) and periods (YYYY-MM).
 * cecks both the format and that the value is a real calendar value
 * (so 2024-04-31 and 2024-13 are rejected). Error messages are user-facing.
 */
public final class InputValidator {

    public static final String INVALID_DATE_MESSAGE =
            "Error: Invalid date provided. Please use the YYYY-MM-DD format with valid values.";
    public static final String INVALID_DATE_RANGE_MESSAGE =
            "Error: Invalid date range. The start date must not be after the end date.";
    // Appendix A only specifies an "invalid" + "date" error, so the period messages
    // use the word "date" too (a month is still a date value).
    public static final String INVALID_PERIOD_MESSAGE =
            "Error: Invalid date provided. Please use the YYYY-MM format with valid values.";
    public static final String INVALID_PERIOD_RANGE_MESSAGE =
            "Error: Invalid date range. The start period must not be after the end period.";

    // exact shapes first, so things like "2024-1-5" or "24-01-05" are rejected
    // even though the parser might be lenient about them.
    private static final Pattern DATE_SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern PERIOD_SHAPE = Pattern.compile("\\d{4}-\\d{2}");

    // STRICT resolver: a day that does not exist in that month is an error, not rounded.
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter PERIOD_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);

    private InputValidator() { }

    // parses a YYYY-MM-DD date; throws if the format or the date is invalid
    public static LocalDate parseDate(String text) throws InvalidInputException {
        if (text == null || !DATE_SHAPE.matcher(text.trim()).matches()) {
            throw new InvalidInputException(INVALID_DATE_MESSAGE);
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(INVALID_DATE_MESSAGE);
        }
    }

    // parses a YYYY-MM period; throws if the format or the month is invalid
    public static YearMonth parsePeriod(String text) throws InvalidInputException {
        if (text == null || !PERIOD_SHAPE.matcher(text.trim()).matches()) {
            throw new InvalidInputException(INVALID_PERIOD_MESSAGE);
        }
        try {
            return YearMonth.parse(text.trim(), PERIOD_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(INVALID_PERIOD_MESSAGE);
        }
    }

    // throws unless start is on or before end
    public static void requireDateOrder(LocalDate start, LocalDate end) throws InvalidInputException {
        if (start.isAfter(end)) {
            throw new InvalidInputException(INVALID_DATE_RANGE_MESSAGE);
        }
    }

    // throws unless start is on or before end
    public static void requirePeriodOrder(YearMonth start, YearMonth end) throws InvalidInputException {
        if (start.isAfter(end)) {
            throw new InvalidInputException(INVALID_PERIOD_RANGE_MESSAGE);
        }
    }
}
