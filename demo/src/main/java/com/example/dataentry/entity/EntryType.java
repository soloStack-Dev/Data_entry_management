package com.example.dataentry.entity;

/**
 * Controlled list of entry types.
 *
 * <p>Spec 03 section 6 requires a fixed set of values instead of free text. Modelling this as an
 * enum means the database, the Thymeleaf {@code <select>} and Java code all agree on the same
 * values, and an unknown value coming from a hand-crafted request is rejected by the binder instead
 * of silently reaching the database.
 *
 * <p>{@link #getLabel()} provides the human readable text used in the UI, so controllers and
 * templates never hard-code the display names.
 */
public enum EntryType {

	PURCHASE("Purchase"),
	SALE("Sale"),
	TRANSFER("Transfer"),
	RETURN("Return"),
	OTHER("Other");

	/** Human readable text shown in the drop-down and in the collection table. */
	private final String label;

	EntryType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

}
