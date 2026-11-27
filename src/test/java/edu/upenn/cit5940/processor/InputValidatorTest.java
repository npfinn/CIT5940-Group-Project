package edu.upenn.cit5940.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class InputValidatorTest {

    private static void badDate(String text) {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> InputValidator.parseDate(text), "should reject: " + text);
        assertEquals(InputValidator.INVALID_DATE_MESSAGE, e.getMessage());
    }

    private static void badPeriod(String text) {
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> InputValidator.parsePeriod(text), "should reject: " + text);
        assertEquals(InputValidator.INVALID_PERIOD_MESSAGE, e.getMessage());
    }

    @Test
    void parsesValidDates() throws Exception {
        assertEquals(LocalDate.of(2024, 2, 29), InputValidator.parseDate("2024-02-29")); // leap year
        assertEquals(LocalDate.of(2022, 7, 8), InputValidator.parseDate(" 2022-07-08 "));
    }

    @Test
    void rejectsImpossibleDates() {
        badDate("2024-04-31");
        badDate("2023-02-29");   // not a leap year
        badDate("2024-13-01");
        badDate("2024-00-10");
        badDate("2024-01-00");
    }

    @Test
    void rejectsWrongDateFormats() {
        for (String s : new String[] {"", "abc", "2024-1-5", "24-01-05", "2024/01/05",
                "20240105", "2024-01", "2024-01-05-01", "2024-01-5x", null}) {
            badDate(s);
        }
    }

    @Test
    void parsesValidPeriods() throws Exception {
        assertEquals(YearMonth.of(2024, 12), InputValidator.parsePeriod("2024-12"));
        assertEquals(YearMonth.of(2022, 1), InputValidator.parsePeriod("2022-01"));
    }

    @Test
    void rejectsInvalidPeriods() {
        for (String s : new String[] {"2024-13", "2024-00", "2024-1", "24-01", "2024",
                "2024-01-05", "abc", "", null}) {
            badPeriod(s);
        }
    }

    @Test
    void dateOrder() {
        LocalDate a = LocalDate.of(2024, 1, 1), b = LocalDate.of(2024, 1, 2);
        assertDoesNotThrow(() -> InputValidator.requireDateOrder(a, b));
        assertDoesNotThrow(() -> InputValidator.requireDateOrder(a, a)); // equal is allowed
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> InputValidator.requireDateOrder(b, a));
        assertEquals(InputValidator.INVALID_DATE_RANGE_MESSAGE, e.getMessage());
    }

    @Test
    void periodOrder() {
        YearMonth a = YearMonth.of(2024, 1), b = YearMonth.of(2024, 2);
        assertDoesNotThrow(() -> InputValidator.requirePeriodOrder(a, b));
        assertDoesNotThrow(() -> InputValidator.requirePeriodOrder(a, a));
        assertThrows(InvalidInputException.class, () -> InputValidator.requirePeriodOrder(b, a));
    }

    @Test
    void errorMessagesMeetAppendixKeywords() {
        // Appendix A: invalid-date errors must contain "invalid" and "date"
        String m = InputValidator.INVALID_DATE_MESSAGE.toLowerCase();
        assertEquals(true, m.contains("invalid") && m.contains("date"));
        String r = InputValidator.INVALID_DATE_RANGE_MESSAGE.toLowerCase();
        assertEquals(true, r.contains("invalid") && r.contains("date"));
        // the period errors (topics/trends) must satisfy the same keywords
        for (String msg : new String[] {InputValidator.INVALID_PERIOD_MESSAGE,
                InputValidator.INVALID_PERIOD_RANGE_MESSAGE}) {
            String lower = msg.toLowerCase();
            assertEquals(true, lower.contains("invalid") && lower.contains("date"), msg);
        }
    }
}
