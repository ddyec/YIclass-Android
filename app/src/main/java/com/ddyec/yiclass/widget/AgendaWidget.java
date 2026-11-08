package com.ddyec.yiclass.widget;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
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
    LocalDate today = LocalDate.now(settings.zone());
    List<Item> all;
    try (Store store = new Store(c)) {
      all = store.snapshot().items();
    }
    StringBuilder body = new StringBuilder();
    int count = 0;
    for (int d = 0; d < 7 && count < 5; d++) {
      LocalDate date = today.plusDays(d);
      for (Item item : Schedule.on(all, date, settings.term())) {
        if (item.done) continue;
        if (d == 0
            && item.end
                < LocalTime.now(settings.zone()).getHour() * 60
                    + LocalTime.now(settings.zone()).getMinute()) continue;
        if (count++ >= 5) break;
        body.append(d == 0 ? "今天" : date.getMonthValue() + "/" + date.getDayOfMonth())
            .append("  ")
            .append(Schedule.time(item.start))
            .append("  ")
            .append(item.title)
            .append('\n')
            .append("       ")
            .append(item.location.isEmpty() ? "未设置地点" : item.location)
            .append("\n\n");
      }
    }
    if (body.length() == 0) body.append("接下来七天没有安排\n打开 YI课表，留一点时间给自己。");
    for (int id : ids) {
      RemoteViews view = new RemoteViews(c.getPackageName(), R.layout.agenda_widget);
      view.setTextViewText(
          R.id.widget_title, "YI课表 · " + today.getMonthValue() + "月" + today.getDayOfMonth() + "日");
      view.setTextViewText(R.id.widget_content, body.toString().trim());
      view.setOnClickPendingIntent(
          R.id.widget_root,
          PendingIntent.getActivity(
              c,
              id,
              new Intent(c, MainActivity.class),
              PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
      manager.updateAppWidget(id, view);
    }
  }
}
