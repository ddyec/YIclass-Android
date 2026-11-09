package com.ddyec.yiclass.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.view.View;
import android.widget.RemoteViews;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.ui.MainActivity;
import java.time.*;
import java.util.*;

public class AgendaWidget extends AppWidgetProvider {
  public static void refreshAll(Context c) {
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    int[] ids = manager.getAppWidgetIds(new ComponentName(c, AgendaWidget.class));
    if (ids.length > 0) new AgendaWidget().onUpdate(c, manager, ids);
  }

  @Override
  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    Settings settings = new Settings(c);
    updateAt(c, manager, ids, ZonedDateTime.now(settings.zone()));
  }

  public static void updateAt(Context c, AppWidgetManager manager, int[] ids, ZonedDateTime now) {
    Settings settings = new Settings(c);
    LocalDate today = now.toLocalDate();
    int nowMinute = now.getHour() * 60 + now.getMinute();
    List<Item> all;
    try (Store store = new Store(c)) {
      all = store.snapshot().items();
    }

    LocalDate displayDate = today;
    Item primary = null;
    List<Item> remainingToday = new ArrayList<>();
    for (Item item : Schedule.on(all, today, settings.term())) {
      if (!item.done && item.end > nowMinute) remainingToday.add(item);
    }
    remainingToday.sort(
        (a, b) -> {
          boolean aActive = a.start <= nowMinute;
          boolean bActive = b.start <= nowMinute;
          if (aActive != bActive) return aActive ? -1 : 1;
          return Integer.compare(a.start, b.start);
        });

    boolean isToday = true;
    if (!remainingToday.isEmpty()) {
      primary = remainingToday.get(0);
    } else {
      for (int d = 1; d < 7; d++) {
        LocalDate date = today.plusDays(d);
        List<Item> dayItems = new ArrayList<>();
        for (Item item : Schedule.on(all, date, settings.term())) {
          if (!item.done) dayItems.add(item);
        }
        if (!dayItems.isEmpty()) {
          displayDate = date;
          primary = dayItems.get(0);
          isToday = false;
          break;
        }
      }
    }

    String statusText;
    String dateText = displayDate.getMonthValue() + "月" + displayDate.getDayOfMonth() + "日";
    if (primary != null) {
      if (isToday) {
        statusText = primary.start <= nowMinute ? "正在上课" : "下一节";
      } else if (displayDate.equals(today.plusDays(1))) {
        statusText = "明日下一节";
      } else {
        statusText = "近期课程";
      }
    } else {
      int[] ends = settings.ends();
      int cutoff = ends.length > 0 ? ends[ends.length - 1] : 1240;
      boolean showTomorrow = nowMinute >= cutoff;
      LocalDate emptyDate = showTomorrow ? today.plusDays(1) : today;
      statusText = showTomorrow ? "明日课程" : "今日课程";
      dateText = emptyDate.getMonthValue() + "月" + emptyDate.getDayOfMonth() + "日";
    }

    for (int id : ids) {
      RemoteViews view = new RemoteViews(c.getPackageName(), R.layout.agenda_widget);
      view.setTextViewText(R.id.widget_title, statusText);
      view.setTextViewText(R.id.widget_date, dateText);
      if (primary != null) {
        view.setTextViewText(R.id.widget_content, primary.title);
        view.setTextViewText(
            R.id.widget_time, Schedule.time(primary.start) + "–" + Schedule.time(primary.end));
        view.setViewVisibility(R.id.widget_time, View.VISIBLE);
        String loc =
            primary.location.isEmpty()
                ? (isToday && primary.start <= nowMinute ? "正在上课" : "地点待定")
                : primary.location;
        view.setTextViewText(R.id.widget_location, loc);
        view.setViewVisibility(R.id.widget_location, View.VISIBLE);
        if (isToday && remainingToday.size() > 1) {
          Item next = remainingToday.get(1);
          view.setTextViewText(
              R.id.widget_more,
              "后续  "
                  + Schedule.time(next.start)
                  + " "
                  + next.title
                  + (next.location.isEmpty() ? "" : " · " + next.location));
          view.setViewVisibility(R.id.widget_more, View.VISIBLE);
        } else {
          view.setViewVisibility(R.id.widget_more, View.GONE);
        }
      } else {
        view.setTextViewText(
            R.id.widget_content, "明日课程".equals(statusText) ? "明天没有课程" : "今天没有更多课程");
        view.setViewVisibility(R.id.widget_time, View.GONE);
        view.setViewVisibility(R.id.widget_location, View.GONE);
        view.setViewVisibility(R.id.widget_more, View.GONE);
      }
      view.setOnClickPendingIntent(
          R.id.widget_root,
          PendingIntent.getActivity(
              c,
              id,
              new Intent(c, MainActivity.class).putExtra("date", displayDate.toString()),
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
      manager.updateAppWidget(id, view);
    }
  }
}
