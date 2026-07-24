package com.atoss.capacityplanning.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class QuarterTest {

  @Test
  void parseReadsYearAndQuarter() {
    Quarter quarter = Quarter.parse("2026-Q3");

    assertThat(quarter.year()).isEqualTo(2026);
    assertThat(quarter.quarterOfYear()).isEqualTo(3);
  }

  @Test
  void parseRejectsInvalidFormats() {
    assertThatThrownBy(() -> Quarter.parse("2026-13")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Quarter.parse("2026-Q5")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Quarter.parse("Q1-2026")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Quarter.parse("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ofMapsEachMonthToTheRightQuarter() {
    assertThat(Quarter.of(LocalDate.of(2026, 1, 1))).isEqualTo(new Quarter(2026, 1));
    assertThat(Quarter.of(LocalDate.of(2026, 3, 31))).isEqualTo(new Quarter(2026, 1));
    assertThat(Quarter.of(LocalDate.of(2026, 4, 1))).isEqualTo(new Quarter(2026, 2));
    assertThat(Quarter.of(LocalDate.of(2026, 7, 23))).isEqualTo(new Quarter(2026, 3));
    assertThat(Quarter.of(LocalDate.of(2026, 12, 31))).isEqualTo(new Quarter(2026, 4));
  }

  @Test
  void containsMatchesDatesWithinTheSameQuarter() {
    Quarter q3 = Quarter.parse("2026-Q3");

    assertThat(q3.contains(LocalDate.of(2026, 8, 15))).isTrue();
    assertThat(q3.contains(LocalDate.of(2026, 7, 1))).isTrue();
    assertThat(q3.contains(LocalDate.of(2026, 9, 30))).isTrue();
    assertThat(q3.contains(LocalDate.of(2026, 10, 1))).isFalse();
    assertThat(q3.contains(LocalDate.of(2025, 8, 15))).isFalse();
  }

  @Test
  void containsReturnsFalseForNullDate() {
    assertThat(Quarter.parse("2026-Q3").contains(null)).isFalse();
  }

  @Test
  void toStringRendersCanonicalForm() {
    assertThat(new Quarter(2027, 1)).hasToString("2027-Q1");
  }
}
