package com.example.dataentry.exception;

/**
 * Thrown when a record is requested by an id that does not exist.
 *
 * <p>A dedicated exception type (instead of a generic exception) lets
 * {@code GlobalExceptionHandler} turn it into a friendly "not found" message, and keeps the
 * "missing record" case distinguishable from a genuine server fault in the logs.
 */
public class EntryNotFoundException extends RuntimeException {

	public EntryNotFoundException(String message) {
		super(message);
	}

}
