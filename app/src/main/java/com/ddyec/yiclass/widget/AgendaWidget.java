package com.ddyec.yiclass.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.ui.MainActivity;
import java.time.*;
import java.util.*;

public class AgendaWidget extends AppWidgetProvider {
  private static final String[] DAYS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
  private static final int[] ROW_IDS = {
    R.id.widget_row_0,
    R.id.widget_row_1,
    R.id.widget_row_2,
    R.id.widget_row_3,
    R.id.widget_row_4,
    R.id.widget_row_5
  };
  private static final int[] ROW_TIME_IDS = {
    R.id.widget_row_0_time,
    R.id.widget_row_1_time,
    R.id.widget_row_2_time,
    R.id.widget_row_3_time,
    R.id.widget_row_4_time,
    R.id.widget_row_5_time
  };
  private static final int[] ROW_TITLE_IDS = {
    R.id.widget_row_0_title,
    R.id.widget_row_1_title,
    R.id.widget_row_2_title,
    R.id.widget_row_3_title,
    R.id.widget_row_4_title,
    R.id.widget_row_5_title
  };
  private static final int[] ROW_LOC_IDS = {
    R.id.widget_row_0_loc,
    R.id.widget_row_1_loc,
    R.id.widget_row_2_loc,
    R.id.widget_row_3_loc,
    R.id.widget_row_4_loc,
    R.id.widget_row_5_loc
  };

  public static void refreshAll(Context c) {
    AppWidgetManager manager = AppWidgetManager.getInstance(c);
    Settings settings = new Settings(c);
    ZonedDateTime now = ZonedDateTime.now(settings.zone());
    Class<?>[] providers = {
      AgendaWidget.class, AgendaMediumWidget.class, AgendaLargeWidget.class
    };
    for (Class<?> cls : providers) {
      int[] ids = manager.getAppWidgetIds(new ComponentName(c, cls));
      if (ids != null && ids.length > 0) {
        updateAt(c, manager, ids, now);
      }
    }
  }

  @Override
  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    Settings settings = new Settings(c);
    updateAt(c, manager, ids, ZonedDateTime.now(settings.zone()));
  }

  @Override
  public void onAppWidgetOptionsChanged(
      Context c, AppWidgetManager manager, int appWidgetId, Bundle newOptions) {
    Settings settings = new Settings(c);
    updateAt(c, manager, new int[] {appWidgetId}, ZonedDateTime.now(settings.zone()));
  }

  public static void updateAt(Context c, AppWidgetManager manager, int[] ids, ZonedDateTime now) {
    Settings settings = new Settings(c);
    LocalDate today = now.toLocalDate();
    int nowMinute = now.getHour() * 60 + now.getMinute();
    List<Item> all;
    try (Store store = new Store(c)) {
      all = store.snapshot().items();
    }

    List<Item> allToday = new ArrayList<>();
    for (Item item : Schedule.on(all, today, settings.term())) {
      if (!item.done) allToday.add(item);
    }

    Item activeCourse = null;
    List<Item> upcomingToday = new ArrayList<>();
    for (Item item : allToday) {
      if (item.start <= nowMinute && nowMinute < item.end) {
        if (activeCourse == null) activeCourse = item;
      } else if (item.start > nowMinute) {
        upcomingToday.add(item);
      }
    }

    int[] ends = settings.ends();
    int dailyCutoff = ends.length > 0 ? ends[ends.length - 1] : 1240;
    boolean debugTomorrow = settings.widgetDebugTomorrow();
    if (debugTomorrow) {
      activeCourse = null;
    }
    boolean showTomorrow = debugTomorrow || (activeCourse == null && nowMinute >= dailyCutoff);

    LocalDate displayDate = showTomorrow ? today.plusDays(1) : today;
    List<Item> coursesToShow = new ArrayList<>();
    if (activeCourse == null) {
      if (!showTomorrow) {
        coursesToShow.addAll(upcomingToday);
      } else {
        for (Item item : Schedule.on(all, displayDate, settings.term())) {
          if (!item.done) coursesToShow.add(item);
        }
        if (debugTomorrow && coursesToShow.isEmpty()) {
          coursesToShow.addAll(buildDebugTomorrowCourses(all, displayDate));
        }
      }
    }

    String statusText;
    if (activeCourse != null) {
      statusText = "正在上课";
    } else if (!showTomorrow) {
      statusText =
          coursesToShow.isEmpty() ? "今日课程" : "今日待上 · " + coursesToShow.size() + "节";
    } else if (debugTomorrow) {
      statusText = "明日课程 · " + coursesToShow.size() + "节 (调试)";
    } else {
      statusText =
          coursesToShow.isEmpty() ? "明日课程" : "明日课程 · " + coursesToShow.size() + "节";
    }
    String dateText =
        displayDate.getMonthValue()
            + "月"
            + displayDate.getDayOfMonth()
            + "日 "
            + DAYS[displayDate.getDayOfWeek().getValue()];

    for (int id : ids) {
      RemoteViews view = new RemoteViews(c.getPackageName(), R.layout.agenda_widget);
      view.setTextViewText(R.id.widget_title, statusText);
      view.setTextViewText(R.id.widget_date, dateText);

      Bundle opts = manager.getAppWidgetOptions(id);
      int minH = opts != null ? opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
      if (minH <= 0) {
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
        if (info != null && info.provider != null) {
          String clsName = info.provider.getShortClassName();
          if (clsName.contains("Large")) minH = 220;
          else if (clsName.contains("Medium")) minH = 150;
          else minH = 150;
        }
      }
      int maxRows;
      if (minH > 0 && minH < 130) {
        maxRows = 2;
      } else if (minH > 0 && minH < 195) {
        maxRows = 3;
      } else if (minH > 0 && minH < 250) {
        maxRows = 4;
      } else {
        maxRows = ROW_IDS.length;
      }

      if (activeCourse != null) {
        view.setViewVisibility(R.id.widget_active_box, View.VISIBLE);
        view.setViewVisibility(R.id.widget_list_box, View.GONE);
        view.setTextViewText(R.id.widget_content, activeCourse.title);
        view.setTextViewText(
            R.id.widget_time,
            Schedule.time(activeCourse.start) + "–" + Schedule.time(activeCourse.end));
        view.setViewVisibility(R.id.widget_time, View.VISIBLE);
        String loc = formatSub(activeCourse, "正在上课");
        view.setTextViewText(R.id.widget_location, loc);
        view.setViewVisibility(R.id.widget_location, View.VISIBLE);
      } else if (!coursesToShow.isEmpty()) {
        Item first = coursesToShow.get(0);
        view.setTextViewText(R.id.widget_content, first.title);
        view.setTextViewText(
            R.id.widget_time, Schedule.time(first.start) + "–" + Schedule.time(first.end));
        view.setTextViewText(R.id.widget_location, formatSub(first, "地点待定"));

        view.setViewVisibility(R.id.widget_active_box, View.GONE);
        view.setViewVisibility(R.id.widget_list_box, View.VISIBLE);
        int visibleLimit = Math.min(coursesToShow.size(), maxRows);
        for (int i = 0; i < ROW_IDS.length; i++) {
          if (i < visibleLimit) {
            Item item = coursesToShow.get(i);
            view.setViewVisibility(ROW_IDS[i], View.VISIBLE);
            view.setTextViewText(
                ROW_TIME_IDS[i], Schedule.time(item.start) + "–" + Schedule.time(item.end));
            view.setTextViewText(ROW_TITLE_IDS[i], item.title);
            view.setTextViewText(ROW_LOC_IDS[i], formatSub(item, "地点待定"));
          } else {
            view.setViewVisibility(ROW_IDS[i], View.GONE);
          }
        }
      } else {
        view.setViewVisibility(R.id.widget_active_box, View.VISIBLE);
        view.setViewVisibility(R.id.widget_list_box, View.GONE);
        view.setTextViewText(
            R.id.widget_content, showTomorrow ? "明日没有课程安排" : "今日没有更多课程");
        view.setViewVisibility(R.id.widget_time, View.GONE);
        if (!showTomorrow) {
          view.setTextViewText(
              R.id.widget_location, Schedule.time(dailyCutoff) + " 晚课结束后展示明日课程");
          view.setViewVisibility(R.id.widget_location, View.VISIBLE);
        } else {
          view.setViewVisibility(R.id.widget_location, View.GONE);
        }
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

  private static List<Item> buildDebugTomorrowCourses(List<Item> all, LocalDate tomorrow) {
    int targetDay = tomorrow.getDayOfWeek().getValue();
    List<Item> sameWeekday = new ArrayList<>();
    for (Item item : all) {
      if (!"EVENT".equals(item.type) && item.day == targetDay) {
        sameWeekday.add(item);
      }
    }
    if (!sameWeekday.isEmpty()) {
      sameWeekday.sort(Comparator.comparingInt(a -> a.start));
      return sameWeekday;
    }
    for (int d = 1; d <= 7; d++) {
      List<Item> dayList = new ArrayList<>();
      for (Item item : all) {
        if (!"EVENT".equals(item.type) && item.day == d) {
          dayList.add(item);
        }
      }
      if (dayList.size() >= 2) {
        dayList.sort(Comparator.comparingInt(a -> a.start));
        return dayList;
      }
    }
    List<Item> mock = new ArrayList<>();
    Object[][] rows = {
      {"大学英语(一)", 510, 610, "致明楼_501", "郭梦莹"},
      {"普通化学实验", 650, 750, "化工学院楼(盐湖大楼)普通化学实验室", "包新颜"},
      {"高等数学", 840, 940, "菌格楼_2号多媒体教室", "梁昌弘"}
    };
    for (Object[] r : rows) {
      Item i = new Item();
      i.title = (String) r[0];
      i.day = targetDay;
      i.start = (int) r[1];
      i.end = (int) r[2];
      i.location = (String) r[3];
      i.teacher = (String) r[4];
      mock.add(i);
    }
    return mock;
  }

  private static String formatSub(Item item, String fallback) {
    String loc = item.location == null ? "" : item.location.trim();
    String teacher = item.teacher == null ? "" : item.teacher.trim();
    if (!loc.isEmpty() && !teacher.isEmpty()) return loc + " · " + teacher;
    if (!loc.isEmpty()) return loc;
    if (!teacher.isEmpty()) return teacher;
    return fallback;
  }
}
