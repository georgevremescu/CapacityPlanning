package com.atoss.capacityplanning.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

// Derived concept, not a stored entity - computed from a date (e.g. "2026-Q1").
// TODO: simple calendar quarter, no fiscal-year offset support.
public record Quarter(int year, int quarterOfYear) {

  private static final Pattern PATTERN = Pattern.compile("^(\\d{4})-Q([1-4])$");

  // Hardcoded constant, same for every quarter - no holiday calendar.
  // TODO: real/regional working-day calendar.
  public static final int WORKING_DAYS_IN_QUARTER = 62;

  public static Quarter parse(String value) {
    var matcher = PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid quarter format, expected e.g. '2026-Q1': " + value);
    }
    return new Quarter(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
  }

  public static Quarter of(LocalDate date) {
    int quarterOfYear = (date.getMonthValue() - 1) / 3 + 1;
    return new Quarter(date.getYear(), quarterOfYear);
  }

  public boolean contains(LocalDate date) {
    if (date == null) {
      return false;
    }
    return equals(of(date));
  }

  @Override
  public String toString() {
    return year + "-Q" + quarterOfYear;
  }
}
