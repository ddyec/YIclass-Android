package com.ddyec.yiclass.core;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public final class Schedule {
  public static final int[] STARTS = {510, 565, 650, 705, 840, 895, 960, 1015, 1140, 1195};
  public static final int[] ENDS = {555, 610, 695, 750, 885, 940, 1005, 1060, 1185, 1240};

  private Schedule() {}

  public static int week(LocalDate date, LocalDate start) {
    return (int) Math.floorDiv(ChronoUnit.DAYS.between(start, date), 7) + 1;
  }

  public static List<Item> on(Collection<Item> items, LocalDate d, LocalDate start) {
    List<Item> found = new ArrayList<>();
    for (Item i : items) if (i.on(d, start)) found.add(i);
    found.sort(Comparator.comparingInt(i -> i.start));
    return found;
  }

  public static boolean overlaps(Item a, Item b) {
    return a.start < b.end && b.start < a.end;
  }

  public static List<String> conflicts(Collection<Item> items, LocalDate start) {
    List<Item> list = new ArrayList<>(items);
    Set<String> result = new LinkedHashSet<>();
    for (int i = 0; i < list.size(); i++)
      for (int j = i + 1; j < list.size(); j++) {
        Item a = list.get(i), b = list.get(j);
        if (a.done || b.done || !overlaps(a, b)) continue;
        LocalDate date = null;
        if (!a.date.isEmpty()) {
          LocalDate d = LocalDate.parse(a.date);
          if (a.on(d, start) && b.on(d, start)) date = d;
        } else if (!b.date.isEmpty()) {
          LocalDate d = LocalDate.parse(b.date);
          if (a.on(d, start) && b.on(d, start)) date = d;
        } else if (a.day == b.day)
          for (int w : Weeks.parse(a.weeks)) {
            LocalDate d = start.plusWeeks(w - 1).plusDays(a.day - 1);
            if (a.on(d, start) && b.on(d, start)) {
              date = d;
              break;
            }
          }
        if (date != null) result.add(date + "：" + a.title + " 与 " + b.title + " 时间冲突");
      }
    return new ArrayList<>(result);
  }

  public static String time(int minute) {
    return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
  }

  public static int minute(String text) {
    if ("24:00".equals(text)) return 1440;
    LocalTime t = LocalTime.parse(text);
    return t.getHour() * 60 + t.getMinute();
  }

  public static Instant reminder(Item item, LocalDate date, ZoneId zone) {
    return date.atTime(LocalTime.MIDNIGHT.plusMinutes(item.start))
        .atZone(zone)
        .minusMinutes(item.reminder)
        .toInstant();
  }
}
