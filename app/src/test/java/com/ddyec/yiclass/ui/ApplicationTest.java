package com.ddyec.yiclass.ui;

import static org.junit.Assert.*;

import android.app.Application;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import java.io.*;
import java.time.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "zh-rCN-w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ApplicationTest {
  Context context;

  @Before
  public void setup() {
    context = RuntimeEnvironment.getApplication();
    context.deleteDatabase("yiclass.db");
    context.getSharedPreferences("preferences", 0).edit().clear().commit();
  }

  private Item course() {
    Item i = new Item();
    i.title = "大学体育（篮球1）";
    i.day = 3;
    i.weeks = "4-15";
    i.start = 840;
    i.end = 940;
    i.location = "未排地点";
    i.color = 2;
    return i;
  }

  private List<Item> sampleSchedule() {
    List<Item> list = new ArrayList<>();
    Object[][] rows = {
      {"普通化学II", 1, "4-17周", 510, 610, "致明楼_507", "包新颜", 0},
      {"新时代实践教育", 1, "4-17周", 960, 1060, "致明楼_2号多媒体教室", "陈海兵", 2},
      {"大学英语(一)", 2, "4-17周", 510, 610, "致明楼_501", "刘丽丽", 1},
      {"普通化学实验", 2, "5-12周", 650, 750, "化工学院楼(盐湖大楼)普通化学实验室", "包新颜", 3},
      {"高等数学", 2, "4-17周", 840, 940, "菌格楼_2号多媒体教室", "赵文才", 4},
      {"生物学基础II", 3, "4-17周", 510, 610, "启真楼_503", "刘同军", 5},
      {"人工智能通识", 3, "4-15周", 650, 750, "致明楼_微机室一", "陈磊", 0},
      {"大学体育(篮球1)", 3, "4-15周", 840, 940, "未排地点", "杜震", 1},
      {"习近平新时代中国特色社会主义思想概论", 3, "10-13周", 1140, 1240, "未排地点", "李华", 3},
      {"大学英语(一)", 4, "4-17周", 650, 750, "致明楼_501", "刘丽丽", 1},
      {"大学语文", 4, "4-17周", 840, 940, "启真楼_203", "张敏", 2},
      {"习近平新时代中国特色社会主义思想概论", 4, "4-17周", 960, 1060, "菌格楼_3号多媒体教室", "李华", 3},
      {"高等数学", 5, "4-17周", 510, 610, "菌格楼_2号多媒体教室", "赵文才", 4},
      {"习近平新时代中国特色社会主义思想概论", 5, "4-17周", 840, 940, "菌格楼_2号多媒体教室", "李华", 3},
    };
    for (Object[] r : rows) {
      Item i = new Item();
      i.title = (String) r[0];
      i.day = (int) r[1];
      i.weeks = (String) r[2];
      i.start = (int) r[3];
      i.end = (int) r[4];
      i.location = (String) r[5];
      i.teacher = (String) r[6];
      i.color = (int) r[7];
      list.add(i);
    }
    return list;
  }

  @Test
  public void databasePersistsAndRejectsStaleWrites() {
    try (Store db = new Store(context)) {
      var first = db.snapshot();
      db.replace(first.revision(), List.of(course()));
      assertEquals(1, db.snapshot().items().size());
      try {
        db.replace(first.revision(), List.of());
        fail("stale write accepted");
      } catch (IllegalStateException expected) {
      }
    }
    try (Store db = new Store(context)) {
      assertEquals("大学体育（篮球1）", db.snapshot().items().get(0).title);
    }
  }

  @Test
  public void backupRoundTripPreservesExceptionsAndDriftsTermMonday() {
    Settings settings = new Settings(context);
    settings.put("term", "2026-09-03");
    Item c = course();
    c.excludedDates.add("2026-11-04");
    String json = Backup.export(new Store.Snapshot(0, List.of(c)), settings);
    var decoded = Backup.decode(json);
    assertEquals("2026-08-31", decoded.term());
    assertTrue(decoded.items().get(0).excludedDates.contains("2026-11-04"));
    assertFalse(json.contains("API"));
  }

  @Test
  public void weekDayListAgendaSettingsAndEditorsRender() throws Exception {
    Settings settings = new Settings(context);
    settings.put("term", "2026-08-31");
    List<Item> items = sampleSchedule();
    Item event = course();
    event.id = UUID.randomUUID().toString();
    event.type = "EVENT";
    event.title = "复习与整理本周笔记";
    event.date = "2026-11-09";
    event.start = 1080;
    event.end = 1140;
    items.add(event);
    try (Store db = new Store(context)) {
      db.replace(0, items);
    }
    var controller = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = controller.get();
    a.selected = LocalDate.of(2026, 11, 9);
    a.monday = LocalDate.of(2026, 11, 9);
    a.render();
    assertTrue(hasTextInTree(a.body, "夜间无课"));
    assertFalse(hasTextInTree(a.body, "夜间有课"));
    capture(a, "week-light");

    // Wednesday (2026-11-11) has an evening class (19:00-20:40):
    // Must STILL stay collapsed by default and show "夜间有课 · 点击展开"
    a.selected = LocalDate.of(2026, 11, 11);
    a.evening = null;
    a.render();
    assertTrue(hasTextInTree(a.body, "夜间有课 · 点击展开"));
    a.evening = true;
    a.render();
    assertTrue(hasTextInTree(a.body, "夜间有课 · 点击收起"));
    a.evening = null;
    a.selected = LocalDate.of(2026, 11, 9);
    a.mode = 1;
    a.render();
    capture(a, "day-light");
    a.mode = 2;
    a.render();
    capture(a, "list-light");
    a.page = 1;
    a.render();
    capture(a, "agenda-light");
    a.page = 2;
    a.render();
    capture(a, "settings-light");
    new Editors(a).edit(null, null, false);
    new SettingsScreen(a).term();
    new SettingsScreen(a).slots();
    new SettingsScreen(a).api();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertNotNull(a.root);
    assertEquals(items.size(), a.snapshot.items().size());
    controller.pause().stop().destroy();
  }

  @Test
  public void androidPdfBoxReadsActualRotatedDocumentWithoutDiamonds() throws Exception {
    String source = System.getProperty("fixture.pdf", "");
    Assume.assumeFalse(source.isEmpty());
    var imported =
        new com.ddyec.yiclass.importer.PdfImporter()
            .read(context, android.net.Uri.fromFile(new File(source)));
    assertEquals(21, imported.items().size());
    for (Item item : imported.items()) {
      assertFalse(item.title.contains("\uFFFD"));
      assertFalse(item.title.contains("◆"));
      assertFalse(item.title.contains("◇"));
      assertEquals("", item.notes);
    }
    Item basketball =
        imported.items().stream().filter(i -> i.title.contains("篮球")).findFirst().orElseThrow();
    assertEquals(3, basketball.day);
    assertEquals(840, basketball.start);
    assertEquals(940, basketball.end);
    assertEquals("4-15周", basketball.weeks);
    new Settings(context).put("term", "2026-08-31");
    try (Store db = new Store(context)) {
      db.replace(0, imported.items());
    }
    var controller = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = controller.get();
    a.selected = LocalDate.of(2026, 11, 9);
    a.monday = LocalDate.of(2026, 11, 9);
    a.render();
    capture(a, "real-pdf-week");
    a.mode = 1;
    a.render();
    capture(a, "real-pdf-day");
    controller.pause().stop().destroy();
  }

  @Test
  public void remindersDeliverOnlyOnceAndWidgetHandlesActiveUpcomingAndTomorrow() throws Exception {
    Settings settings = new Settings(context);
    settings.put("term", LocalDate.now(settings.zone()).with(DayOfWeek.MONDAY).toString());
    LocalDateTime start =
        LocalDateTime.now(settings.zone()).plusMinutes(14).withSecond(0).withNano(0);
    Item event = course();
    event.type = "EVENT";
    event.title = "普通化学II";
    event.location = "致明楼_507";
    event.date = start.toLocalDate().toString();
    event.start = start.getHour() * 60 + start.getMinute();
    event.end = Math.min(1440, event.start + 100);
    event.reminder = 15;
    try (Store db = new Store(context)) {
      db.replace(0, List.of(event));
    }
    org.robolectric.Shadows.shadowOf((Application) context)
        .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS);
    com.ddyec.yiclass.reminder.Reminders.refresh(context);
    var alarm =
        org.robolectric.Shadows.shadowOf(context.getSystemService(android.app.AlarmManager.class))
            .getNextScheduledAlarm();
    assertNotNull(alarm);
    assertTrue(alarm.triggerAtTime > System.currentTimeMillis());
    long at = Schedule.reminder(event, start.toLocalDate(), settings.zone()).toEpochMilli();
    com.ddyec.yiclass.reminder.Reminders.deliver(context, at);
    com.ddyec.yiclass.reminder.Reminders.deliver(context, at);
    assertEquals(
        1,
        org.robolectric.Shadows.shadowOf(
                context.getSystemService(android.app.NotificationManager.class))
            .getAllNotifications()
            .size());
    var widgets =
        org.robolectric.Shadows.shadowOf(android.appwidget.AppWidgetManager.getInstance(context));
    int id =
        widgets.createWidget(
            com.ddyec.yiclass.widget.AgendaWidget.class, com.ddyec.yiclass.R.layout.agenda_widget);
    try (Store db = new Store(context)) {
      settings.put("term", "2026-08-31");
      db.replace(db.snapshot().revision(), sampleSchedule());
    }
    // 1. In class (09:00 Monday): Exclusive active course mode
    com.ddyec.yiclass.widget.AgendaWidget.updateAt(
        context,
        android.appwidget.AppWidgetManager.getInstance(context),
        new int[] {id},
        ZonedDateTime.of(2026, 11, 9, 9, 0, 0, 0, settings.zone()));
    View widgetView = widgets.getViewFor(id);
    android.widget.TextView meta = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_title);
    android.widget.TextView content =
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_content);
    assertEquals("正在上课", meta.getText().toString());
    assertEquals("普通化学II", content.getText().toString());
    assertEquals(
        View.VISIBLE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_active_box).getVisibility());
    assertEquals(
        View.GONE, widgetView.findViewById(com.ddyec.yiclass.R.id.widget_list_box).getVisibility());
    captureView(widgetView, 220, 180, "widget-small-active");
    copyScreenshotToDrawable("widget-small-active", "widget_preview_small");

    // 2. Before class (08:09 Tuesday): Show all 3 upcoming courses of Tuesday filling space
    com.ddyec.yiclass.widget.AgendaWidget.updateAt(
        context,
        android.appwidget.AppWidgetManager.getInstance(context),
        new int[] {id},
        ZonedDateTime.of(2026, 11, 10, 8, 9, 0, 0, settings.zone()));
    widgetView = widgets.getViewFor(id);
    meta = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_title);
    assertEquals("今日待上 · 3节", meta.getText().toString());
    assertEquals(
        View.GONE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_active_box).getVisibility());
    assertEquals(
        View.VISIBLE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_list_box).getVisibility());
    android.widget.TextView row1Loc =
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_row_1_loc);
    assertTrue(row1Loc.getText().toString().contains("化工学院楼(盐湖大楼)普通化学实验室"));
    captureView(widgetView, 340, 165, "widget-light");
    for (int i = 0; i < 3; i++) {
      View row = widgetView.findViewById(
          new int[] {
            com.ddyec.yiclass.R.id.widget_row_0,
            com.ddyec.yiclass.R.id.widget_row_1,
            com.ddyec.yiclass.R.id.widget_row_2
          }[i]);
      View loc = widgetView.findViewById(
          new int[] {
            com.ddyec.yiclass.R.id.widget_row_0_loc,
            com.ddyec.yiclass.R.id.widget_row_1_loc,
            com.ddyec.yiclass.R.id.widget_row_2_loc
          }[i]);
      assertTrue(
          "Row " + i + " loc bottom (" + loc.getBottom() + ") overflows row height (" + row.getHeight() + ")",
          loc.getBottom() <= row.getHeight());
    }
    captureView(widgetView, 340, 165, "widget-medium");
    captureView(widgetView, 340, 260, "widget-large");
    copyScreenshotToDrawable("widget-medium", "widget_preview_medium");
    copyScreenshotToDrawable("widget-large", "widget_preview_large");

    // 3. After today's courses end but BEFORE evening last period ends (18:00 Monday, cutoff 20:40):
    // Must show "今日没有更多课程" and NEVER switch to tomorrow early!
    com.ddyec.yiclass.widget.AgendaWidget.updateAt(
        context,
        android.appwidget.AppWidgetManager.getInstance(context),
        new int[] {id},
        ZonedDateTime.of(2026, 11, 9, 18, 0, 0, 0, settings.zone()));
    widgetView = widgets.getViewFor(id);
    meta = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_title);
    content = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_content);
    assertEquals("今日课程", meta.getText().toString());
    assertEquals("今日没有更多课程", content.getText().toString());
    assertEquals(
        View.VISIBLE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_active_box).getVisibility());
    assertEquals(
        View.GONE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_list_box).getVisibility());

    // 4. AFTER evening last period ends (20:45 Monday, cutoff 20:40):
    // Now switch to tomorrow's courses!
    com.ddyec.yiclass.widget.AgendaWidget.updateAt(
        context,
        android.appwidget.AppWidgetManager.getInstance(context),
        new int[] {id},
        ZonedDateTime.of(2026, 11, 9, 20, 45, 0, 0, settings.zone()));
    widgetView = widgets.getViewFor(id);
    meta = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_title);
    assertEquals("明日课程 · 3节", meta.getText().toString());

    // 5. Debug switch ON ("widget_debug_tomorrow" = "1"):
    // Even at 09:00 Monday (in-class time) or 12:00 Monday (before evening cutoff),
    // must force showing tomorrow's courses for visual container/overflow inspection!
    settings.put("widget_debug_tomorrow", "1");
    com.ddyec.yiclass.widget.AgendaWidget.updateAt(
        context,
        android.appwidget.AppWidgetManager.getInstance(context),
        new int[] {id},
        ZonedDateTime.of(2026, 11, 9, 9, 0, 0, 0, settings.zone()));
    widgetView = widgets.getViewFor(id);
    meta = widgetView.findViewById(com.ddyec.yiclass.R.id.widget_title);
    assertEquals("明日课程 · 3节 (调试)", meta.getText().toString());
    assertEquals(
        View.GONE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_active_box).getVisibility());
    assertEquals(
        View.VISIBLE,
        widgetView.findViewById(com.ddyec.yiclass.R.id.widget_list_box).getVisibility());
    settings.put("widget_debug_tomorrow", "0");
  }

  private void copyScreenshotToDrawable(String screenshotName, String drawableName) throws Exception {
    File src = new File("build/screenshots/" + screenshotName + ".png");
    File dst = new File("src/main/res/drawable-xxxhdpi/" + drawableName + ".png");
    dst.getParentFile().mkdirs();
    if (src.exists()) {
      java.nio.file.Files.copy(
          src.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Test
  @Config(qualifiers = "zh-rCN-w360dp-h800dp-night-mdpi")
  public void darkThemeAndAssistantRender() throws Exception {
    new Settings(context).put("term", "2026-08-31");
    try (Store db = new Store(context)) {
      db.replace(0, sampleSchedule());
    }
    var controller = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = controller.get();
    a.selected = LocalDate.of(2026, 11, 9);
    a.monday = LocalDate.of(2026, 11, 9);
    a.render();
    capture(a, "week-dark");
    new AssistantSheet(a).show();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    controller.pause().stop().destroy();
  }

  private boolean hasTextInTree(View view, String target) {
    if (view instanceof android.widget.TextView tv && tv.getText() != null) {
      if (tv.getText().toString().contains(target)) return true;
    }
    if (view instanceof android.view.ViewGroup group) {
      for (int i = 0; i < group.getChildCount(); i++) {
        if (hasTextInTree(group.getChildAt(i), target)) return true;
      }
    }
    return false;
  }

  private void invalidateTree(View view) {
    view.invalidate();
    if (view instanceof android.view.ViewGroup group)
      for (int i = 0; i < group.getChildCount(); i++) invalidateTree(group.getChildAt(i));
  }

  private void captureView(View view, int w, int h, String name) throws Exception {
    int scale = 4;
    view.measure(
        View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
    view.layout(0, 0, w, h);
    invalidateTree(view);
    Bitmap image = Bitmap.createBitmap(w * scale, h * scale, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(image);
    canvas.scale(scale, scale);
    view.draw(canvas);
    File output = new File("build/screenshots/" + name + ".png");
    output.getParentFile().mkdirs();
    try (OutputStream stream = new FileOutputStream(output)) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
    image.recycle();
  }

  private void capture(MainActivity a, String name) throws Exception {
    View root = a.getWindow().getDecorView();
    root.measure(
        View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(915, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 412, 915);
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    root.measure(
        View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(915, View.MeasureSpec.EXACTLY));
    root.layout(0, 0, 412, 915);
    assertTrue(
        "Navigation extends beyond root",
        a.root.getChildAt(a.root.getChildCount() - 1).getBottom() <= a.root.getHeight());
    invalidateTree(root);
    Bitmap image = Bitmap.createBitmap(412, 915, Bitmap.Config.ARGB_8888);
    root.draw(new Canvas(image));
    File output = new File("build/screenshots/" + name + ".png");
    output.getParentFile().mkdirs();
    try (OutputStream stream = new FileOutputStream(output)) {
      assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
    }
    image.recycle();
  }
}
