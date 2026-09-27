package com.example.dataentry.service;

import java.util.List;

import com.example.dataentry.entity.DataEntry;

/**
 * Immutable snapshot of the three dashboard cards (spec 01 section 3).
 *
 * <p>A Java {@code record} fits perfectly: this is read-only data that only travels from the
 * service to the view, so it needs no setters and no no-argument constructor.
 *
 * @param totalEntries  every row in {@code data_entries}
 * @param todaysEntries rows whose {@code entry_date} falls on the current day
 * @param recentEntries the five most recently created rows, newest first
 */
public record DashboardStats(long totalEntries, long todaysEntries, List<DataEntry> recentEntries) {
}
