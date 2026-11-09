package com.ddyec.yiclass.core;

import java.time.LocalDate;
import java.util.*;

public final class Item {
  public String id = UUID.randomUUID().toString();
  public String title = "", location = "", teacher = "", notes = "";
  public String type = "COURSE", weeks = "1-20", date = "";
  public int day = 1, start = 510, end = 555, color = 0, reminder = 15;
  public boolean done = false;
  public Set<String> excludedDates = new HashSet<>();

  public void validate() {
    if (id == null
        || id.trim().isEmpty()
        || title == null
        || title.trim().isEmpty()
        || title.length() > 160) throw new IllegalArgumentException("请填写课程或日程名称（最多160字）");
    if (location == null || teacher == null || notes == null || excludedDates == null)
      throw new IllegalArgumentException("课程字段不完整");
    if (!Set.of("COURSE", "EVENT").contains(type)) throw new IllegalArgumentException("未知日程类型");
    if (day < 1 || day > 7 || start < 0 || end > 1440 || start >= end)
      throw new IllegalArgumentException("请检查星期和起止时间");
    if (reminder < -1 || reminder > 1440)
      throw new IllegalArgumentException("提醒提前量应为0–1440分钟，-1为关闭");
    if (date == null || weeks == null) throw new IllegalArgumentException("日期或周次缺失");
    if (!date.isEmpty()) LocalDate.parse(date);
    else Weeks.parse(weeks);
    for (String d : excludedDates) LocalDate.parse(d);
  }

  public boolean on(LocalDate d, LocalDate termStart) {
    if (excludedDates.contains(d.toString())) return false;
    if (!date.isEmpty()) return date.equals(d.toString());
    int week = Schedule.week(d, termStart);
    return d.getDayOfWeek().getValue() == day && Weeks.parse(weeks).contains(week);
  }

  public String fingerprint() {
    return title
        + "|"
        + location
        + "|"
        + teacher
        + "|"
        + type
        + "|"
        + day
        + "|"
        + start
        + "|"
        + end
        + "|"
        + (date.isEmpty() ? Weeks.canonical(weeks) : "")
        + "|"
        + date;
  }
}
