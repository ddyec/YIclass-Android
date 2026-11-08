package com.ddyec.yiclass.core;

import static org.junit.Assert.*;

import java.time.*;
import java.util.*;
import org.junit.Test;

public class ScheduleTest {
  private final LocalDate term = LocalDate.of(2026, 8, 31);

  private Item item(String weeks, int start, int end) {
    Item i = new Item();
    i.title = "测试课程";
    i.weeks = weeks;
    i.start = start;
    i.end = end;
    return i;
  }

  @Test
  public void parsesParityAndDiscontinuousWeeks() {
    assertEquals(Set.of(1, 3, 5, 8, 10), Weeks.parse("1-5(单),8,10"));
    assertEquals(Set.of(2, 4, 6), Weeks.parse("第2-6周（双）"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsInvalidWeek() {
    Weeks.parse("0-16");
  }

  @Test
  public void semesterBoundariesAndExceptions() {
    Item i = item("1-16", 510, 555);
    assertFalse(i.on(term.minusWeeks(1), term));
    assertTrue(i.on(term, term));
    i.excludedDates.add(term.toString());
    assertFalse(i.on(term, term));
    assertEquals(0, Schedule.week(term.minusDays(1), term));
  }

  @Test
  public void conflictsRespectParityBoundariesAndDates() {
    Item a = item("1-15(单)", 510, 555), b = item("2-16(双)", 520, 570);
    assertTrue(Schedule.conflicts(List.of(a, b), term).isEmpty());
    b.weeks = "1-16";
    assertEquals(1, Schedule.conflicts(List.of(a, b), term).size());
    b.start = 555;
    assertTrue(Schedule.conflicts(List.of(a, b), term).isEmpty());
    b.date = "2027-06-01";
    b.start = 510;
    assertTrue(Schedule.conflicts(List.of(a, b), term).isEmpty());
  }

  @Test
  public void reminderUsesExplicitTimezoneAndPriorDay() {
    Item i = item("1", 10, 50);
    i.reminder = 30;
    assertEquals(
        Instant.parse("2026-08-30T15:40:00Z"),
        Schedule.reminder(i, term, ZoneId.of("Asia/Shanghai")));
  }

  @Test
  public void reminderKeepsWallClockTimeAcrossDaylightSavingsChange() {
    Item i = item("1", 510, 555);
    i.reminder = 15;
    assertEquals(
        Instant.parse("2026-03-08T12:15:00Z"),
        Schedule.reminder(i, LocalDate.of(2026, 3, 8), ZoneId.of("America/New_York")));
    assertEquals(1440, Schedule.minute("24:00"));
  }

  @Test
  public void aiMovePreservesOtherOccurrencesAndDoesNotMutateSnapshot() {
    Item base = item("1-16", 510, 555);
    Plan p = new Plan();
    Plan.Operation op = new Plan.Operation();
    op.action = "update";
    op.id = base.id;
    op.scopeDate = "2026-08-31";
    op.item = item("1-16", 600, 645);
    op.item.date = "2026-09-01";
    p.operations.add(op);
    var next = p.preview(List.of(base), term);
    assertEquals(2, next.size());
    assertTrue(base.excludedDates.isEmpty());
    assertTrue(Schedule.on(next, term, term).isEmpty());
    assertEquals(1, Schedule.on(next, term.plusDays(1), term).size());
    assertEquals(1, Schedule.on(next, term.plusWeeks(1), term).size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsHallucinatedAiIds() {
    Plan p = new Plan();
    Plan.Operation o = new Plan.Operation();
    o.action = "delete";
    o.id = "missing";
    p.operations.add(o);
    p.preview(List.of(), term);
  }
}
