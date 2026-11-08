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
  public void backupRoundTripPreservesExceptionsAndSemester() {
    Settings settings = new Settings(context);
    settings.put("term", "2026-08-31");
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
    try (Store db = new Store(context)) {
      List<Item> items = new ArrayList<>();
      String[] names = {
        "普通化学Ⅱ", "大学英语（一）", "生物学基础Ⅱ", "人工智能通识", "高等数学", "大学语文", "习近平新时代中国特色社会主义思想概论"
      };
      for (int d = 1; d <= 5; d++) {
        Item i = course();
        i.id = UUID.randomUUID().toString();
        i.title = names[d - 1];
        i.day = d;
        i.start = 510;
        i.end = 610;
        i.location = "致明楼 · 501";
        i.color = d;
        items.add(i);
      }
      items.add(course());
      Item event = course();
      event.id = UUID.randomUUID().toString();
      event.type = "EVENT";
      event.title = "复习与整理本周笔记";
      event.date = "2026-11-04";
      event.start = 1080;
      event.end = 1140;
      items.add(event);
      db.replace(0, items);
    }
    var controller = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = controller.get();
    a.selected = LocalDate.of(2026, 11, 4);
    a.monday = LocalDate.of(2026, 11, 2);
    a.render();
    capture(a, "week-light");
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
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertNotNull(a.root);
    assertEquals(7, a.snapshot.items().size());
    controller.pause().stop().destroy();
  }

  @Test
  public void androidPdfBoxReadsActualRotatedDocument() throws Exception {
    String source = System.getProperty("fixture.pdf", "");
    Assume.assumeFalse(source.isEmpty());
    var imported =
        new com.ddyec.yiclass.importer.PdfImporter()
            .read(context, android.net.Uri.fromFile(new File(source)));
    assertEquals(21, imported.items().size());
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
    a.selected = LocalDate.of(2026, 11, 4);
    a.monday = LocalDate.of(2026, 11, 2);
    a.render();
    capture(a, "real-pdf-week");
    a.mode = 1;
    a.render();
    capture(a, "real-pdf-day");
    controller.pause().stop().destroy();
  }

  @Test
  public void remindersDeliverOnlyOnceAndWidgetUsesSameSchedule() {
    Settings settings = new Settings(context);
    settings.put("term", LocalDate.now(settings.zone()).with(DayOfWeek.MONDAY).toString());
    LocalDateTime start =
        LocalDateTime.now(settings.zone()).plusMinutes(14).withSecond(0).withNano(0);
    Item event = course();
    event.type = "EVENT";
    event.date = start.toLocalDate().toString();
    event.start = start.getHour() * 60 + start.getMinute();
    event.end = Math.min(1440, event.start + 30);
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
    android.widget.TextView content =
        widgets.getViewFor(id).findViewById(com.ddyec.yiclass.R.id.widget_content);
    assertTrue(content.getText().toString().contains(event.title));
  }

  @Test
  @Config(qualifiers = "zh-rCN-w360dp-h800dp-night-mdpi")
  public void darkThemeAndAssistantRender() throws Exception {
    new Settings(context).put("term", "2026-08-31");
    try (Store db = new Store(context)) {
      db.replace(0, List.of(course()));
    }
    var controller = Robolectric.buildActivity(MainActivity.class).setup();
    MainActivity a = controller.get();
    a.selected = LocalDate.of(2026, 11, 4);
    a.monday = LocalDate.of(2026, 11, 2);
    a.render();
    capture(a, "week-dark");
    new AssistantSheet(a).show();
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    controller.pause().stop().destroy();
  }

  private void invalidateTree(View view) {
    view.invalidate();
    if (view instanceof android.view.ViewGroup group)
      for (int i = 0; i < group.getChildCount(); i++) invalidateTree(group.getChildAt(i));
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
    System.out.println(
        name
            + " decor="
            + root.getWidth()
            + "x"
            + root.getHeight()
            + " app="
            + a.root.getWidth()
            + "x"
            + a.root.getHeight()
            + " body="
            + a.body.getHeight()
            + " bottom="
            + a.root.getChildAt(2).getTop()
            + ":"
            + a.root.getChildAt(2).getBottom());
    assertTrue(
        "Navigation extends beyond root", a.root.getChildAt(2).getBottom() <= a.root.getHeight());
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
