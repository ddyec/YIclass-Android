package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.*;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.reminder.Reminders;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
  final ExecutorService worker = Executors.newSingleThreadExecutor();
  Store store;
  Settings settings;
  Store.Snapshot snapshot;
  LinearLayout root;
  FrameLayout body;
  int page = 0, mode = 0;
  LocalDate selected, monday;
  boolean evening = false;
  final ActivityResultLauncher<String[]> pdfPicker =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri != null) new ImportFlow(this).pdf(uri);
          });
  final ActivityResultLauncher<String[]> backupPicker =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri != null) new ImportFlow(this).restore(uri);
          });
  final ActivityResultLauncher<String[]> pdfViewer =
      registerForActivityResult(
          new ActivityResultContracts.OpenDocument(),
          uri -> {
            if (uri != null) new PdfPreview(this).show(uri);
          });
  final ActivityResultLauncher<String> backupWriter =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("application/json"),
          uri -> {
            if (uri != null) new ImportFlow(this).export(uri);
          });
  final ActivityResultLauncher<String> notificationPermission =
      registerForActivityResult(
          new ActivityResultContracts.RequestPermission(),
          granted -> {
            refresh();
          });

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    settings = new Settings(this);
    store = new Store(this);
    selected = LocalDate.now(settings.zone());
    if (getIntent().hasExtra("date"))
      try {
        selected = LocalDate.parse(getIntent().getStringExtra("date"));
      } catch (Exception ignored) {
      }
    if (state != null) {
      selected = LocalDate.parse(state.getString("selected", selected.toString()));
      mode = state.getInt("mode");
      page = state.getInt("page");
      evening = state.getBoolean("evening");
    }
    monday =
        state != null
            ? LocalDate.parse(state.getString("monday", selected.with(DayOfWeek.MONDAY).toString()))
            : selected.with(DayOfWeek.MONDAY);
    WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    buildShell();
    refresh();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (store != null) refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    out.putString("selected", selected.toString());
    out.putString("monday", monday.toString());
    out.putInt("mode", mode);
    out.putInt("page", page);
    out.putBoolean("evening", evening);
  }

  @Override
  protected void onDestroy() {
    worker.shutdown();
    store.close();
    super.onDestroy();
  }

  void buildShell() {
    root = Ui.column(this);
    root.setBackgroundColor(Ui.surface(this));
    ViewCompat.setOnApplyWindowInsetsListener(
        root,
        (view, insets) -> {
          var b =
              insets.getInsets(
                  WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
          view.setPadding(b.left, b.top, b.right, b.bottom);
          return insets;
        });
    MaterialToolbar toolbar = new MaterialToolbar(this);
    toolbar.setTitle("YI课表");
    toolbar.setSubtitle("把时间，留给重要的事");
    android.graphics.drawable.Drawable logo =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.brand_logo);
    if (logo != null) {
      android.graphics.Bitmap bitmap =
          ((android.graphics.drawable.BitmapDrawable) logo).getBitmap();
      android.graphics.drawable.BitmapDrawable sized =
          new android.graphics.drawable.BitmapDrawable(
              getResources(),
              android.graphics.Bitmap.createScaledBitmap(
                  bitmap, Ui.dp(this, 32), Ui.dp(this, 32), true));
      sized.setTargetDensity(getResources().getDisplayMetrics());
      toolbar.setLogo(sized);
    }
    toolbar.setTitleTextColor(Ui.ink(this));
    toolbar.setSubtitleTextColor(Ui.muted(this));
    toolbar.getMenu().add(0, 1, 0, "导入").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    toolbar
        .getMenu()
        .add(0, 2, 1, "新增")
        .setIcon(R.drawable.ic_add)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    toolbar.setOnMenuItemClickListener(
        item -> {
          if (item.getItemId() == 1) importMenu();
          else new Editors(this).edit(null, null, false);
          return true;
        });
    root.addView(toolbar);
    body = new FrameLayout(this);
    root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

    LinearLayout bottom = Ui.row(this);
    bottom.setGravity(Gravity.CENTER_VERTICAL);
    bottom.setBackground(Ui.roundedBg(this, Ui.surfaceLow(this), 28, 1, Ui.outlineVariant(this)));
    bottom.setPadding(Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 14), Ui.dp(this, 2));
    BottomNavigationView nav = new BottomNavigationView(this);
    nav.setBackgroundColor(Color.TRANSPARENT);
    nav.setElevation(0);
    nav.setItemActiveIndicatorColor(ColorStateList.valueOf(Ui.primaryContainer(this)));
    nav.getMenu().add(0, 10, 0, "课表").setIcon(R.drawable.ic_calendar);
    nav.getMenu().add(0, 11, 1, "日程").setIcon(R.drawable.ic_tasks);
    nav.getMenu().add(0, 12, 2, "设置").setIcon(R.drawable.ic_settings);
    nav.setLabelVisibilityMode(
        com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
    nav.setSelectedItemId(10 + page);
    nav.setOnItemSelectedListener(
        item -> {
          page = item.getItemId() - 10;
          render();
          return true;
        });
    bottom.addView(nav, new LinearLayout.LayoutParams(0, Ui.dp(this, 74), 1));
    FloatingActionButton assistant = new FloatingActionButton(this);
    assistant.setImageResource(R.drawable.ic_book);
    assistant.setBackgroundTintList(ColorStateList.valueOf(Ui.primaryContainer(this)));
    assistant.setImageTintList(ColorStateList.valueOf(Ui.onPrimaryContainer(this)));
    assistant.setCompatElevation(0);
    assistant.setContentDescription("打开AI课表助手");
    assistant.setOnClickListener(v -> new AssistantSheet(this).show());
    LinearLayout.LayoutParams fabLp = new LinearLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56));
    fabLp.leftMargin = Ui.dp(this, 6);
    bottom.addView(assistant, fabLp);
    root.addView(bottom);
    setContentView(root);
  }

  void importMenu() {
    new MaterialAlertDialogBuilder(this)
        .setTitle("导入与备份")
        .setItems(
            new String[] {"从本地 PDF 导入课表", "手动添加课程", "恢复 JSON 备份", "导出 JSON 备份", "阅读本地 PDF"},
            (d, n) -> {
              switch (n) {
                case 0 -> pdfPicker.launch(new String[] {"application/pdf"});
                case 1 -> new Editors(this).edit(null, null, false);
                case 2 ->
                    backupPicker.launch(
                        new String[] {
                          "application/json", "text/plain", "application/octet-stream"
                        });
                case 3 -> backupWriter.launch("YI课表-" + LocalDate.now() + ".json");
                case 4 -> pdfViewer.launch(new String[] {"application/pdf"});
              }
            })
        .show();
  }

  void refresh() {
    snapshot = store.snapshot();
    render();
    worker.execute(
        () -> {
          try {
            Reminders.refresh(getApplicationContext());
          } catch (Exception e) {
            runOnUiThread(() -> toast("提醒更新失败：" + e.getMessage()));
          }
        });
  }

  void render() {
    if (body == null || snapshot == null) return;
    body.removeAllViews();
    if (page == 0) schedule();
    else if (page == 1) agenda();
    else body.addView(new SettingsScreen(this).build());
  }

  void save(long revision, List<Item> items) {
    try {
      store.replace(revision, items);
      refresh();
      toast("已保存");
    } catch (Exception e) {
      error(e);
    }
  }

  void error(Exception e) {
    if (isFinishing() || isDestroyed()) return;
    new MaterialAlertDialogBuilder(this)
        .setTitle("暂时无法完成")
        .setMessage(e.getMessage() == null ? "请检查输入后重试" : e.getMessage())
        .setPositiveButton("知道了", null)
        .show();
  }

  void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  void review(String title, Store.Snapshot base, List<Item> next, String detail, Runnable apply) {
    List<String> conflicts = Schedule.conflicts(next, settings.term());
    String text =
        detail
            + (conflicts.isEmpty()
                ? "\n\n未检测到时间冲突。"
                : "\n\n发现时间冲突：\n" + String.join("\n", conflicts));
    new MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setMessage(text)
        .setNegativeButton("取消", null)
        .setPositiveButton(
            conflicts.isEmpty() ? "确认保存" : "仍然保存",
            (d, w) -> {
              if (store.snapshot().revision() != base.revision()) {
                error(new IllegalStateException("课表已变化，请重新预览"));
                return;
              }
              apply.run();
            })
        .show();
  }

  void schedule() {
    LinearLayout content = Ui.column(this);
    body.addView(content, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout week = Ui.row(this);
    week.setPadding(Ui.dp(this, 14), Ui.dp(this, 2), Ui.dp(this, 14), Ui.dp(this, 2));
    MaterialButton prevBtn = Ui.tonalButton(this, "‹", () -> shift(-1));
    week.addView(prevBtn, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 44)));

    int w = Schedule.week(monday, settings.term());
    LinearLayout centerTitle = Ui.column(this);
    centerTitle.setGravity(Gravity.CENTER);
    TextView label =
        Ui.text(
            this,
            (w < 1 ? "学期前" : w > 30 ? "学期后" : "第 " + w + " 周")
                + "  ·  "
                + monday.getMonthValue()
                + "月"
                + monday.getDayOfMonth()
                + "日",
            17,
            Ui.ink(this));
    label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    label.setGravity(Gravity.CENTER);
    centerTitle.addView(label);
    week.addView(centerTitle, new LinearLayout.LayoutParams(0, -2, 1));

    MaterialButton todayBtn =
        Ui.tonalButton(
            this,
            "本周",
            () -> {
              selected = LocalDate.now(settings.zone());
              monday = selected.with(DayOfWeek.MONDAY);
              render();
            });
    LinearLayout.LayoutParams todayLp =
        new LinearLayout.LayoutParams(Ui.dp(this, 68), Ui.dp(this, 44));
    todayLp.rightMargin = Ui.dp(this, 6);
    week.addView(todayBtn, todayLp);

    MaterialButton nextBtn = Ui.tonalButton(this, "›", () -> shift(1));
    week.addView(nextBtn, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 44)));
    content.addView(week);

    MaterialButtonToggleGroup toggle = new MaterialButtonToggleGroup(this);
    toggle.setSingleSelection(true);
    toggle.setSelectionRequired(true);
    for (int i = 0; i < 3; i++) {
      final int value = i;
      MaterialButton b =
          new MaterialButton(
              this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
      b.setId(100 + i);
      b.setText(new String[] {"周视图", "日视图", "列表"}[i]);
      b.setCornerRadius(Ui.dp(this, 22));
      b.setTypeface(
          Typeface.create(
              "sans-serif-medium", i == mode ? Typeface.BOLD : Typeface.NORMAL));
      if (i == mode) {
        b.setBackgroundTintList(ColorStateList.valueOf(Ui.primaryContainer(this)));
        b.setTextColor(Ui.onPrimaryContainer(this));
      } else {
        b.setBackgroundTintList(ColorStateList.valueOf(Ui.surfaceLow(this)));
        b.setTextColor(Ui.muted(this));
      }
      toggle.addView(b, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1));
      b.setOnClickListener(
          v -> {
            mode = value;
            render();
          });
    }
    toggle.check(100 + mode);
    LinearLayout.LayoutParams tabs = new LinearLayout.LayoutParams(-1, -2);
    tabs.setMargins(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 8));
    content.addView(toggle, tabs);
    if (snapshot.items().isEmpty()) {
      empty(content, "新的学期，从这里开始", "导入教务系统 PDF，或手动添加第一门课程。你的课表保存在本机。", this::importMenu, "导入课表");
      return;
    }
    if (mode == 0) weekGrid(content);
    else if (mode == 1) dayView(content);
    else listView(content);
  }

  void shift(int n) {
    monday = monday.plusWeeks(n);
    selected = selected.plusWeeks(n);
    render();
  }

  void weekGrid(LinearLayout parent) {
    LinearLayout header = Ui.row(this);
    header.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 4));
    TextView axis = Ui.text(this, "节次", 11, Ui.muted(this));
    axis.setGravity(Gravity.CENTER);
    header.addView(axis, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 46)));
    LocalDate today = LocalDate.now(settings.zone());
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean isToday = date.equals(today);
      TextView h =
          Ui.text(
              this,
              Ui.day(d + 1) + "\n" + date.getDayOfMonth(),
              12,
              isToday ? Ui.onPrimaryContainer(this) : Ui.muted(this));
      h.setGravity(Gravity.CENTER);
      h.setTypeface(
          Typeface.create(
              "sans-serif-medium", isToday ? Typeface.BOLD : Typeface.NORMAL));
      if (isToday) {
        h.setBackground(Ui.roundedBg(this, Ui.primaryContainer(this), 14, 0, 0));
      }
      h.setContentDescription(date + "，查看日课表");
      h.setOnClickListener(
          v -> {
            selected = date;
            mode = 1;
            render();
          });
      LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1);
      hp.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
      header.addView(h, hp);
    }
    parent.addView(header);
    ScrollView vertical = new ScrollView(this);
    parent.addView(vertical, new LinearLayout.LayoutParams(-1, 0, 1));
    FrameLayout canvas = new FrameLayout(this);
    vertical.addView(canvas, new ScrollView.LayoutParams(-1, Ui.dp(this, evening ? 850 : 690)));
    canvas.addOnLayoutChangeListener(
        (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
          if (right - left != oldRight - oldLeft) populateGrid(canvas, evening ? 10 : 8);
        });
    canvas.post(() -> populateGrid(canvas, evening ? 10 : 8));
    boolean hasNight = false;
    for (int d = 0; d < 7; d++)
      if (Schedule.on(snapshot.items(), monday.plusDays(d), settings.term()).stream()
          .anyMatch(i -> i.start >= 1080)) hasNight = true;
    MaterialButton nightBtn =
        Ui.tonalButton(
            this,
            (hasNight ? "有晚间安排" : "晚间无课") + " · " + (evening ? "收起" : "展开"),
            () -> {
              evening = !evening;
              render();
            });
    LinearLayout.LayoutParams nightLp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 44));
    nightLp.setMargins(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4));
    parent.addView(nightBtn, nightLp);
  }

  void populateGrid(FrameLayout canvas, int count) {
    if (canvas.getWidth() == 0) return;
    canvas.removeAllViews();
    int axis = Ui.dp(this, 42), row = Ui.dp(this, 82);
    float width = (canvas.getWidth() - axis) / 7f;
    int[] starts = settings.starts(), ends = settings.ends();
    for (int i = 0; i < count; i++) {
      TextView time =
          Ui.text(
              this,
              (i + 1) + "\n" + Schedule.time(starts[i]) + "\n" + Schedule.time(ends[i]),
              10,
              Ui.muted(this));
      time.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
      FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(axis, row);
      p.topMargin = i * row + Ui.dp(this, 6);
      canvas.addView(time, p);
      View line = new View(this);
      line.setBackgroundColor(Ui.outlineVariant(this));
      FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, 1);
      l.leftMargin = axis;
      l.topMargin = i * row;
      canvas.addView(line, l);
    }
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      List<Item> items = Schedule.on(snapshot.items(), date, settings.term());
      for (Item item : items) {
        if (!evening && item.start >= 1080) continue;
        float top = position(item.start, starts, ends), bottom = position(item.end, starts, ends);
        if (top >= count) continue;
        bottom = Math.min(count, bottom);
        long overlap = items.stream().filter(i -> Schedule.overlaps(i, item)).count();
        int overlapIndex = 0;
        for (Item other : items) {
          if (other == item) break;
          if (Schedule.overlaps(other, item)) overlapIndex++;
        }
        MaterialCardView card = Ui.card(this);
        card.setRadius(Ui.dp(this, 14));
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(Ui.courseColor(this, item.color));
        LinearLayout text = Ui.column(this);
        int pad = Ui.dp(this, 5);
        text.setPadding(pad, pad, pad, pad);

        View accent = new View(this);
        accent.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 99, 0, 0));
        LinearLayout.LayoutParams accentLp =
            new LinearLayout.LayoutParams(Ui.dp(this, 18), Ui.dp(this, 3));
        accentLp.bottomMargin = Ui.dp(this, 3);
        text.addView(accent, accentLp);

        TextView title = Ui.text(this, item.title, 11.5f, Ui.courseInk(this, item.color));
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setMaxLines(4);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        TextView location = Ui.text(this, item.location, 9.5f, Ui.courseInk(this, item.color));
        location.setAlpha(0.78f);
        location.setMaxLines(3);
        location.setEllipsize(TextUtils.TruncateAt.END);
        location.setPadding(0, Ui.dp(this, 2), 0, 0);
        text.addView(location);
        card.addView(text);
        card.setContentDescription(
            item.title
                + "，"
                + date
                + "，"
                + Schedule.time(item.start)
                + "至"
                + Schedule.time(item.end)
                + "，"
                + item.location
                + (overlap > 1 ? "，有时间冲突" : ""));
        card.setOnClickListener(v -> details(item, date));
        int cw = Math.max(1, (int) (width / overlap));
        FrameLayout.LayoutParams p =
            new FrameLayout.LayoutParams(
                cw - Ui.dp(this, 3),
                Math.max(Ui.dp(this, 40), (int) ((bottom - top) * row) - Ui.dp(this, 5)));
        p.leftMargin = axis + (int) (d * width) + overlapIndex * cw + Ui.dp(this, 1);
        p.topMargin = (int) (top * row) + Ui.dp(this, 3);
        canvas.addView(card, p);
      }
    }
  }

  static float position(int minute, int[] starts, int[] ends) {
    for (int i = 0; i < starts.length; i++) {
      if (minute <= starts[i]) return i;
      if (minute <= ends[i]) return i + (minute - starts[i]) / (float) (ends[i] - starts[i]);
    }
    return starts.length;
  }

  void dayView(LinearLayout parent) {
    LinearLayout days = Ui.row(this);
    days.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 4));
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean active = date.equals(selected);
      MaterialButton b =
          Ui.button(
              this,
              Ui.day(d + 1) + "\n" + date.getDayOfMonth(),
              () -> {
                selected = date;
                render();
              });
      b.setTextSize(11);
      b.setCornerRadius(Ui.dp(this, 18));
      b.setPadding(0, 0, 0, 0);
      if (active) {
        b.setStrokeWidth(0);
        b.setBackgroundTintList(ColorStateList.valueOf(Ui.primary(this)));
        b.setTextColor(Ui.onPrimary(this));
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
      }
      LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, Ui.dp(this, 60), 1);
      dlp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
      days.addView(b, dlp);
    }
    parent.addView(days);
    LinearLayout list = Ui.column(this);
    Ui.inset(list, 16);
    parent.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));
    List<Item> items = Schedule.on(snapshot.items(), selected, settings.term());
    LinearLayout headRow = Ui.row(this);
    headRow.addView(
        Ui.heading(
            this,
            selected.getMonthValue()
                + "月"
                + selected.getDayOfMonth()
                + "日  "
                + Ui.day(selected.getDayOfWeek().getValue())),
        new LinearLayout.LayoutParams(0, -2, 1));
    headRow.addView(
        Ui.pillBadge(
            this,
            items.size() + " 项安排",
            Ui.primaryContainer(this),
            Ui.onPrimaryContainer(this)));
    list.addView(headRow);
    if (items.isEmpty())
      empty(
          list,
          "今天没有安排",
          "可以专注学习，也可以给自己留一点空白。",
          () -> new Editors(this).edit(null, selected, true),
          "添加日程");
    else for (Item item : items) list.addView(itemCard(item, selected));
  }

  void listView(LinearLayout parent) {
    LinearLayout list = Ui.column(this);
    Ui.inset(list, 16);
    parent.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));
    List<Item> sorted = new ArrayList<>(snapshot.items());
    sorted.sort(Comparator.comparingInt((Item i) -> i.day).thenComparingInt(i -> i.start));
    list.addView(Ui.note(this, "全部 " + sorted.size() + " 项 · 点按编辑课程与循环规则"));
    for (Item i : sorted) list.addView(itemCard(i, null));
  }

  View itemCard(Item item, LocalDate date) {
    MaterialCardView card = Ui.card(this);
    card.setRadius(Ui.dp(this, 24));
    card.setStrokeWidth(0);
    card.setCardBackgroundColor(Ui.courseColor(this, item.color));

    LinearLayout outer = Ui.row(this);
    Ui.inset(outer, 16);

    View bar = new View(this);
    bar.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 99, 0, 0));
    LinearLayout.LayoutParams barLp =
        new LinearLayout.LayoutParams(Ui.dp(this, 5), Ui.dp(this, 52));
    barLp.rightMargin = Ui.dp(this, 14);
    outer.addView(bar, barLp);

    LinearLayout box = Ui.column(this);
    LinearLayout metaRow = Ui.row(this);
    TextView timeBadge =
        Ui.pillBadge(
            this,
            Schedule.time(item.start) + " — " + Schedule.time(item.end),
            Ui.surfaceLow(this),
            Ui.courseInk(this, item.color));
    metaRow.addView(timeBadge);

    TextView dayLabel =
        Ui.text(
            this,
            "  "
                + (date != null
                    ? Ui.day(date.getDayOfWeek().getValue())
                    : item.date.isEmpty() ? Ui.day(item.day) + " · " + item.weeks : item.date),
            12,
            Ui.courseInk(this, item.color));
    dayLabel.setAlpha(0.82f);
    metaRow.addView(dayLabel);
    box.addView(metaRow);

    TextView title =
        Ui.text(
            this,
            (item.done ? "✓ " : "") + item.title,
            18,
            Ui.courseInk(this, item.color));
    title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    title.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 4));
    box.addView(title);

    TextView sub =
        Ui.text(
            this,
            (item.location.isEmpty() ? "未设置地点" : item.location)
                + (item.teacher.isEmpty() ? "" : " · " + item.teacher)
                + (item.reminder >= 0 ? " · 提前" + item.reminder + "分钟提醒" : ""),
            13,
            Ui.courseInk(this, item.color));
    sub.setAlpha(0.82f);
    box.addView(sub);

    outer.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
    card.addView(outer);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = Ui.dp(this, 10);
    card.setLayoutParams(p);
    card.setOnClickListener(v -> details(item, date));
    return card;
  }

  void details(Item item, LocalDate date) {
    String content =
        (item.date.isEmpty() ? Ui.day(item.day) + " · " + item.weeks : item.date)
            + "\n"
            + Schedule.time(item.start)
            + " — "
            + Schedule.time(item.end)
            + "\n"
            + item.location
            + "\n"
            + item.teacher
            + "\n"
            + item.notes
            + (item.excludedDates.isEmpty()
                ? ""
                : "\n已跳过日期：" + String.join("、", item.excludedDates));
    var dialog =
        new MaterialAlertDialogBuilder(this)
            .setTitle(item.title)
            .setMessage(content)
            .setPositiveButton(
                "编辑",
                (d, w) -> {
                  if (date != null && item.date.isEmpty())
                    new MaterialAlertDialogBuilder(this)
                        .setTitle("编辑范围")
                        .setItems(
                            new String[] {"仅本次（" + date + "）", "整个循环课程"},
                            (dd, n) -> new Editors(this).edit(item, n == 0 ? date : null, false))
                        .show();
                  else new Editors(this).edit(item, null, "EVENT".equals(item.type));
                })
            .setNegativeButton("关闭", null)
            .setNeutralButton("删除", (d, w) -> remove(item, date));
    dialog.show();
  }

  void remove(Item item, LocalDate date) {
    String[] choices =
        date != null && item.date.isEmpty()
            ? new String[] {"仅删除本次（" + date + "）", "删除整个循环课程"}
            : new String[] {"删除这项安排"};
    new MaterialAlertDialogBuilder(this)
        .setTitle("删除 " + item.title)
        .setItems(
            choices,
            (d, n) -> {
              Store.Snapshot base = store.snapshot();
              List<Item> next = new ArrayList<>(base.items());
              for (Item i : next)
                if (i.id.equals(item.id)) {
                  if (choices.length == 2 && n == 0) i.excludedDates.add(date.toString());
                  else next.remove(i);
                  break;
                }
              review("确认删除", base, next, "删除后可通过已有的 JSON 备份恢复。", () -> save(base.revision(), next));
            })
        .setNegativeButton("取消", null)
        .show();
  }

  void agenda() {
    LinearLayout list = Ui.column(this);
    Ui.inset(list, 20);
    body.addView(Ui.scroll(this, list));

    MaterialCardView hero = Ui.heroCard(this);
    LinearLayout heroBox = Ui.column(this);
    Ui.inset(heroBox, 20);

    boolean notifications = NotificationManagerCompat.from(this).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || getSystemService(AlarmManager.class).canScheduleExactAlarms();

    LinearLayout badgeRow = Ui.row(this);
    badgeRow.addView(
        Ui.statusBadge(
            this,
            notifications ? "🔔 通知已开启" : "🔔 通知未开启",
            notifications));
    TextView exactPill =
        Ui.statusBadge(
            this,
            exact ? "⏱ 精确提醒就绪" : "⏱ 近似提醒 · 点按授权",
            exact);
    LinearLayout.LayoutParams epLp = new LinearLayout.LayoutParams(-2, -2);
    epLp.leftMargin = Ui.dp(this, 8);
    exactPill.setOnClickListener(
        v -> {
          page = 2;
          render();
        });
    badgeRow.addView(exactPill, epLp);
    heroBox.addView(badgeRow);

    TextView hTitle = Ui.text(this, "日程与待办", 24, Ui.onPrimaryContainer(this));
    hTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    hTitle.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
    heroBox.addView(hTitle);

    TextView hNote =
        Ui.text(
            this,
            "把计划放进时间里。日程会与课程一起检查冲突并提醒。",
            13,
            Ui.onPrimaryContainer(this));
    hNote.setPadding(0, 0, 0, Ui.dp(this, 12));
    heroBox.addView(hNote);

    heroBox.addView(
        Ui.filledButton(
            this,
            "＋ 新建日程",
            () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true)));
    hero.addView(heroBox);
    LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
    heroLp.bottomMargin = Ui.dp(this, 14);
    list.addView(hero, heroLp);

    if (!notifications || !exact) {
      list.addView(
          Ui.permissionCard(
              this,
              "⚡",
              !notifications ? "开启课前与日程通知权限" : "开启精确分钟提醒权限",
              !notifications
                  ? "当前尚未开启系统通知权限，日程与课程到点时无法弹出提醒。点按一键授权。"
                  : "当前使用系统近似提醒，开启精确闹钟权限后可在锁屏与省电模式下准点唤醒。",
              !notifications ? "待开启通知" : "建议开启",
              false,
              !notifications ? "设置通知权限" : "允许精确提醒",
              () -> {
                if (!notifications && Build.VERSION.SDK_INT >= 33)
                  notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                else new SettingsScreen(this).requestExactAlarmPermission();
              }));
    }

    var events = new ArrayList<Item>();
    for (Item i : snapshot.items()) if ("EVENT".equals(i.type)) events.add(i);
    events.sort(Comparator.comparing((Item i) -> i.date).thenComparingInt(i -> i.start));
    if (events.isEmpty())
      empty(
          list,
          "还没有日程",
          "添加自习、考试、会议或生活计划。",
          () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true),
          "安排一件事");
    for (Item item : events) {
      LinearLayout row = Ui.row(this);
      com.google.android.material.checkbox.MaterialCheckBox check =
          new com.google.android.material.checkbox.MaterialCheckBox(this);
      check.setChecked(item.done);
      check.setContentDescription("标记完成：" + item.title);
      check.setOnCheckedChangeListener(
          (v, done) -> {
            Store.Snapshot base = store.snapshot();
            for (Item i : base.items()) if (i.id.equals(item.id)) i.done = done;
            save(base.revision(), base.items());
          });
      row.addView(check);
      row.addView(itemCard(item, null), new LinearLayout.LayoutParams(0, -2, 1));
      list.addView(row);
    }
  }

  void empty(LinearLayout parent, String title, String note, Runnable action, String button) {
    MaterialCardView card = Ui.surfaceCard(this);
    LinearLayout box = Ui.column(this);
    Ui.inset(box, 24);
    box.setGravity(Gravity.CENTER);
    TextView mark = Ui.text(this, "◷", 44, Ui.primary(this));
    box.addView(mark);
    box.addView(Ui.heading(this, title));
    TextView message = Ui.note(this, note);
    message.setGravity(Gravity.CENTER);
    box.addView(message);
    box.addView(Ui.filledButton(this, button, action));
    card.addView(box);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = Ui.dp(this, 8);
    parent.addView(card, lp);
  }
}
