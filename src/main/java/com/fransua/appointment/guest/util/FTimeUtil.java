package com.fransua.appointment.guest.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class FTimeUtil {

  public record FTimeRange(LocalTime start, LocalTime end) {}

  // S1 < E2 AND S2 < E1
  public static boolean isOverlapping(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
    return s1.isBefore(e2) && s2.isBefore(e1);
  }

  // S1 < E2 AND S2 < E1
  public static boolean isOverlapping(Instant s1, Instant e1, Instant s2, Instant e2) {
    return s1.isBefore(e2) && s2.isBefore(e1);
  }

  // S1 < E2 AND S2 < E1
  public static boolean isOverlapping(FTimeRange tr1, FTimeRange tr2) {
    return tr1.start.isBefore(tr2.end) && tr2.start.isBefore(tr1.end);
  }

  public static boolean containsInclusive(FTimeRange container, FTimeRange content) {
    return !container.start.isAfter(content.start) && !container.end.isBefore(content.end);
  }

  public static boolean containsInclusive(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
    return !s1.isAfter(s2) && !e1.isBefore(e2);
  }

  public static boolean isBetweenExclusive(LocalDate date, LocalDate start, LocalDate end) {
    return start.isBefore(date) && end.isAfter(date);
  }

  public static boolean isBetweenInclusive(LocalDate date, LocalDate start, LocalDate end) {
    return !date.isBefore(start) && !date.isAfter(end);
  }

  public static List<FTimeRange> subtractBusyRangesInclusive(
      List<FTimeRange> allRanges, List<FTimeRange> busyRanges) {
    List<FTimeRange> result = new ArrayList<>(allRanges);

    for (FTimeRange busy : busyRanges) {
      List<FTimeRange> nextIteration = new ArrayList<>();

      for (FTimeRange free : result) {
        if (!isOverlapping(free, busy)) {
          nextIteration.add(free);
        } else {
          if (!free.start().isAfter(busy.start())) {
            FTimeRange part = new FTimeRange(free.start(), busy.start());
            if (!part.start().equals(part.end())) {
              nextIteration.add(part);
            }
          }
          if (!free.end().isBefore(busy.end())) {
            FTimeRange part = new FTimeRange(busy.end(), free.end());
            if (!part.start().equals(part.end())) {
              nextIteration.add(part);
            }
          }
        }
      }
      result = nextIteration;
    }
    return result;
  }

  public static List<FTimeRange> splitByStep(LocalTime start, LocalTime end, Duration step) {
    if (step.isNegative() || step.isZero()) {
      throw new IllegalArgumentException("Step must be positive");
    }

    List<FTimeRange> slots = new ArrayList<>();
    LocalTime current = start;

    while (true) {
      LocalTime next = current.plus(step);

      if (next.isAfter(end) || next.isBefore(current)) {
        break;
      }

      slots.add(new FTimeRange(current, next));
      current = next;
    }
    return slots;
  }
}
