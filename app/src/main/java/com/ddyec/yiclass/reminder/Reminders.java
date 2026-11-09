package com.ddyec.yiclass.reminder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.ui.MainActivity;
import com.ddyec.yiclass.widget.AgendaWidget;
import java.time.*;
import java.util.*;

public final class Reminders {
  public static final String CHANNEL = "classes";
  public static final String LIVE_CHANNEL = "live_schedule";
  private static final int LIVE_NOTIFICATION_ID = 9001;

  private Reminders() {}

  public static void refresh(Context c) {
    var nm = c.getSystemService(NotificationManager.class);
    nm.createNotificationChannel(
        new NotificationChannel(CHANNEL, "上课与日程提醒", NotificationManager.IMPORTANCE_HIGH));
    Settings s = new Settings(c);
    List<Item> items;
    try (Store store = new Store(c)) {
      items = store.snapshot().items();
    }
    Instant now = Instant.now();
    LocalDate today = now.atZone(s.zone()).toLocalDate();
    Instant next = today.plusDays(1).atStartOfDay(s.zone()).toInstant();
    int[] ends = s.ends();
    int dailyCutoff = ends.length > 0 ? ends[ends.length - 1] : 1240;
    Instant cutoffToday =
        today
            .atTime(LocalTime.MIDNIGHT.plusMinutes(Math.min(1439, dailyCutoff)))
            .atZone(s.zone())
            .toInstant();
    if (cutoffToday.isAfter(now) && cutoffToday.isBefore(next)) next = cutoffToday;
    for (int d = 0; d <= 2; d++) {
      LocalDate date = today.plusDays(d);
      for (Item i : Schedule.on(items, date, s.term())) {
        if (i.done) continue;
        if (i.reminder >= 0) {
          Instant at = Schedule.reminder(i, date, s.zone());
          if (at.isAfter(now) && at.isBefore(next)) next = at;
        }
        Instant startAt =
            date.atTime(LocalTime.MIDNIGHT.plusMinutes(i.start)).atZone(s.zone()).toInstant();
        if (startAt.isAfter(now) && startAt.isBefore(next)) next = startAt;
        Instant endAt =
            date.atTime(LocalTime.MIDNIGHT.plusMinutes(Math.min(1439, i.end)))
                .atZone(s.zone())
                .toInstant();
        if (endAt.isAfter(now) && endAt.isBefore(next)) next = endAt;
      }
    }
    var alarm = c.getSystemService(AlarmManager.class);
    Intent intent = new Intent(c, ReminderReceiver.class).putExtra("at", next.toEpochMilli());
    PendingIntent pending =
        PendingIntent.getBroadcast(
            c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    alarm.cancel(pending);
    boolean exact = Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms();
    if (exact)
      alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pending);
    else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toEpochMilli(), pending);
    AgendaWidget.refreshAll(c);
    updateLiveCardAndShortcuts(c, s, items);
  }

  private static void updateLiveCardAndShortcuts(Context c, Settings s, List<Item> all) {
    ZonedDateTime zdt = ZonedDateTime.now(s.zone());
    LocalDate today = zdt.toLocalDate();
    int nowMin = zdt.getHour() * 60 + zdt.getMinute();
    int[] ends = s.ends();
    int dailyCutoff = ends.length > 0 ? ends[ends.length - 1] : 1240;
    List<Item> todayList = new ArrayList<>();
    for (Item item : Schedule.on(all, today, s.term())) {
      if (!item.done) todayList.add(item);
    }
    Item active = null;
    List<Item> upcoming = new ArrayList<>();
    for (Item item : todayList) {
      if (item.start <= nowMin && nowMin < item.end) {
        if (active == null) active = item;
      } else if (item.start > nowMin) {
        upcoming.add(item);
      }
    }
    boolean showTomorrow = active == null && nowMin >= dailyCutoff;
    LocalDate targetDate = showTomorrow ? today.plusDays(1) : today;
    List<Item> displayList = new ArrayList<>();
    if (active == null) {
      if (!showTomorrow) {
        displayList.addAll(upcoming);
      } else {
        for (Item item : Schedule.on(all, targetDate, s.term())) {
          if (!item.done) displayList.add(item);
        }
      }
    }

    try {
      android.content.pm.ShortcutManager sm =
          c.getSystemService(android.content.pm.ShortcutManager.class);
      if (sm != null) {
        List<android.content.pm.ShortcutInfo> shortcuts = new ArrayList<>();
        List<Item> scSource = active != null ? List.of(active) : displayList;
        for (int i = 0; i < Math.min(3, scSource.size()); i++) {
          Item item = scSource.get(i);
          String shortLabel = Schedule.time(item.start) + " " + item.title;
          if (shortLabel.length() > 14) shortLabel = shortLabel.substring(0, 14);
          String longLabel =
              Schedule.time(item.start)
                  + "–"
                  + Schedule.time(item.end)
                  + " "
                  + item.title
                  + (item.location.isEmpty() ? "" : " · " + item.location);
          shortcuts.add(
              new android.content.pm.ShortcutInfo.Builder(c, "course_" + i)
                  .setShortLabel(shortLabel)
                  .setLongLabel(longLabel)
                  .setIcon(
                      android.graphics.drawable.Icon.createWithResource(
                          c, R.drawable.ic_nav_schedule))
                  .setIntent(
                      new Intent(c, MainActivity.class)
                          .setAction(Intent.ACTION_VIEW)
                          .putExtra("date", targetDate.toString()))
                  .build());
        }
        sm.setDynamicShortcuts(shortcuts);
      }
    } catch (Exception ignored) {
    }

    var nm = c.getSystemService(NotificationManager.class);
    if (!s.liveNotificationCard()) {
      nm.cancel(LIVE_NOTIFICATION_ID);
      return;
    }
    if (Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    nm.createNotificationChannel(
        new NotificationChannel(
            LIVE_CHANNEL, "锁屏与通知栏常驻课表卡片", NotificationManager.IMPORTANCE_LOW));
    String title;
    String body;
    if (active != null) {
      title = "正在上课 · " + active.title;
      body =
          Schedule.time(active.start)
              + "–"
              + Schedule.time(active.end)
              + " · "
              + (active.location.isEmpty() ? "正在上课" : active.location);
    } else if (!displayList.isEmpty()) {
      title = (showTomorrow ? "明日课程 · " : "今日待上 · ") + displayList.size() + "节";
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < Math.min(4, displayList.size()); i++) {
        Item item = displayList.get(i);
        if (i > 0) sb.append('\n');
        sb.append(Schedule.time(item.start))
            .append('–')
            .append(Schedule.time(item.end))
            .append("  ")
            .append(item.title);
        if (!item.location.isEmpty()) sb.append(" · ").append(item.location);
      }
      body = sb.toString();
    } else {
      title = showTomorrow ? "明日课程" : "今日课程";
      body = showTomorrow ? "明日没有课程安排" : "今日没有更多课程";
    }
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            LIVE_NOTIFICATION_ID,
            new Intent(c, MainActivity.class).putExtra("date", targetDate.toString()),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    nm.notify(
        LIVE_NOTIFICATION_ID,
        new NotificationCompat.Builder(c, LIVE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build());
  }

  public static void deliver(Context c, long scheduledAt) {
    Settings s = new Settings(c);
    Instant at = Instant.ofEpochMilli(scheduledAt), now = Instant.now();
    if (at.isAfter(now) || Duration.between(at, now).toMinutes() > 60) return;
    if (Build.VERSION.SDK_INT >= 33
        && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return;
    var nm = c.getSystemService(NotificationManager.class);
    try (Store store = new Store(c)) {
      var snapshot = store.snapshot();
      LocalDate day = at.atZone(s.zone()).toLocalDate();
      var prefs = c.getSharedPreferences("delivered", 0);
      for (int d = 0; d <= 1; d++)
        for (Item i : Schedule.on(snapshot.items(), day.plusDays(d), s.term())) {
          LocalDate date = day.plusDays(d);
          if (i.done
              || i.reminder < 0
              || Schedule.reminder(i, date, s.zone()).toEpochMilli() != scheduledAt) continue;
          String token = i.id + ":" + date + ":" + i.start;
          if (prefs.getBoolean(token, false)) continue;
          if (prefs.getAll().size() > 2000) prefs.edit().clear().apply();
          prefs.edit().putBoolean(token, true).apply();
          PendingIntent open =
              PendingIntent.getActivity(
                  c,
                  token.hashCode(),
                  new Intent(c, MainActivity.class).putExtra("date", date.toString()),
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
          nm.notify(
              token.hashCode(),
              new NotificationCompat.Builder(c, CHANNEL)
                  .setSmallIcon(R.drawable.ic_notification)
                  .setContentTitle(i.title)
                  .setContentText(Schedule.time(i.start) + " · " + i.location)
                  .setContentIntent(open)
                  .setAutoCancel(true)
                  .setPriority(NotificationCompat.PRIORITY_HIGH)
                  .build());
        }
    }
  }
}
