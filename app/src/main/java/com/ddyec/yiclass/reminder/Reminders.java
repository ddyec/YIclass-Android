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
    // Midnight refresh keeps the widget and future reminders rolling, even during vacations.
    for (int d = 0; d <= 2; d++)
      for (Item i : Schedule.on(items, today.plusDays(d), s.term())) {
        if (i.done || i.reminder < 0) continue;
        Instant at = Schedule.reminder(i, today.plusDays(d), s.zone());
        if (at.isAfter(now) && at.isBefore(next)) next = at;
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
  }

  public static void deliver(Context c, long scheduledAt) {
    Settings s = new Settings(c);
    Instant at = Instant.ofEpochMilli(scheduledAt), now = Instant.now();
    // Don't fire stale reminders after a device was offline for a long time.
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
