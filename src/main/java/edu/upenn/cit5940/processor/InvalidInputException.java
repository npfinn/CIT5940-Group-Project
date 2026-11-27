package edu.upenn.cit5940.processor;

/**
 * Thrown by the processor tier when user input fails validation (bad date,
 * bad period, start after end). The message is user-facing, so the UI can
 * print it as is.
 */
public class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}
