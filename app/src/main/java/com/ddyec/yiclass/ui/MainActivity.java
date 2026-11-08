package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.*;
import androidx.transition.TransitionManager;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.reminder.Reminders;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
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
  BottomNavigationView nav;
  FloatingActionButton assistantFab;
  int page = 0, mode = 0;
  private int renderedPage = -1, renderedMode = -1;
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
    EdgeToEdge.enable(this);
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
          var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          var ime = insets.getInsets(WindowInsetsCompat.Type.ime());
          // The navigation bar draws behind the gesture area and pads itself (edge-to-edge).
          view.setPadding(bars.left, bars.top, bars.right, ime.bottom);
          return insets;
        });

    // Top app bar – Expressive small app bar with emphasized title.
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
    toolbar
        .getMenu()
        .add(0, 1, 0, "导入与备份")
        .setIcon(R.drawable.ic_upload_file)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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

    // Content stage: page body + the Expressive FAB that opens the AI assistant.
    FrameLayout stage = new FrameLayout(this);
    body = new FrameLayout(this);
    stage.addView(body, new FrameLayout.LayoutParams(-1, -1));
    assistantFab = new FloatingActionButton(this);
    assistantFab.setImageResource(R.drawable.ic_book);
    assistantFab.setContentDescription("打开AI课表助手");
    androidx.appcompat.widget.TooltipCompat.setTooltipText(assistantFab, "AI 课表助手");
    assistantFab.setOnClickListener(v -> new AssistantSheet(this).show());
    FrameLayout.LayoutParams fabLp =
        new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
    fabLp.setMargins(0, 0, Ui.dp(this, 16), Ui.dp(this, 16));
    stage.addView(assistantFab, fabLp);
    root.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));

    // Expressive (flexible) navigation bar with filled/outlined icon states.
    nav = new BottomNavigationView(this);
    nav.getMenu().add(0, 10, 0, "课表").setIcon(R.drawable.ic_nav_schedule_selector);
    nav.getMenu().add(0, 11, 1, "日程").setIcon(R.drawable.ic_nav_agenda_selector);
    nav.getMenu().add(0, 12, 2, "设置").setIcon(R.drawable.ic_nav_settings_selector);
    nav.setLabelVisibilityMode(
        com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED);
    nav.setSelectedItemId(10 + page);
    nav.setOnItemSelectedListener(
        item -> {
          page = item.getItemId() - 10;
          render();
          return true;
        });
    root.addView(nav, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);
  }

  void importMenu() {
    new MaterialAlertDialogBuilder(this)
        .setIcon(R.drawable.ic_upload_file)
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
    if (renderedPage != -1 && (renderedPage != page || renderedMode != mode))
      TransitionManager.beginDelayedTransition(body, new MaterialFadeThrough());
    renderedPage = page;
    renderedMode = mode;
    if (nav != null) {
      MenuItem item = nav.getMenu().findItem(10 + page);
      if (item != null && !item.isChecked()) item.setChecked(true);
    }
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
        .setIcon(R.drawable.ic_privacy_tip)
        .setTitle("暂时无法完成")
        .setMessage(e.getMessage() == null ? "请检查输入后重试" : e.getMessage())
        .setPositiveButton("知道了", null)
        .show();
  }

  /** Snackbar feedback anchored above the FAB / navigation bar. */
  void toast(String s) {
    if (root == null || isFinishing() || isDestroyed()) return;
    Snackbar bar = Snackbar.make(root, s, Snackbar.LENGTH_LONG);
    if (assistantFab != null && assistantFab.isShown()) bar.setAnchorView(assistantFab);
    else if (nav != null) bar.setAnchorView(nav);
    bar.show();
  }

  void review(String title, Store.Snapshot base, List<Item> next, String detail, Runnable apply) {
    List<String> conflicts = Schedule.conflicts(next, settings.term());
    String text =
        detail
            + (conflicts.isEmpty()
                ? "\n\n未检测到时间冲突。"
                : "\n\n发现时间冲突：\n" + String.join("\n", conflicts));
    new MaterialAlertDialogBuilder(this)
        .setIcon(conflicts.isEmpty() ? R.drawable.ic_event_available : R.drawable.ic_privacy_tip)
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

  // ---- Schedule page -----------------------------------------------------------------------

  void schedule() {
    LinearLayout content = Ui.column(this);
    body.addView(content, new FrameLayout.LayoutParams(-1, -1));

    LinearLayout week = Ui.row(this);
    week.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 6));
    int w = Schedule.week(monday, settings.term());
    LocalDate today = LocalDate.now(settings.zone());
    boolean thisWeek = monday.equals(today.with(DayOfWeek.MONDAY));
    LinearLayout titleCol = Ui.column(this);
    TextView label = Ui.display(this, w < 1 ? "学期前" : w > 30 ? "学期后" : "第 " + w + " 周");
    label.setTextSize(28);
    titleCol.addView(label);
    LocalDate sunday = monday.plusDays(6);
    TextView range =
        Ui.styled(
            this,
            monday.getMonthValue()
                + "月"
                + monday.getDayOfMonth()
                + "日 – "
                + sunday.getMonthValue()
                + "月"
                + sunday.getDayOfMonth()
                + "日"
                + (thisWeek ? "  ·  本周" : ""),
            com.google.android.material.R.attr.textAppearanceBodyMedium,
            thisWeek ? Ui.primary(this) : Ui.muted(this));
    titleCol.addView(range);
    week.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));

    week.addView(Ui.iconButton(this, R.drawable.ic_chevron_left, "上一周", true, () -> shift(-1)));
    MaterialButton todayBtn =
        Ui.withIcon(
            Ui.tonalButton(
                this,
                "本周",
                () -> {
                  selected = LocalDate.now(settings.zone());
                  monday = selected.with(DayOfWeek.MONDAY);
                  render();
                }),
            R.drawable.ic_today);
    todayBtn.setEnabled(!thisWeek);
    LinearLayout.LayoutParams todayLp = new LinearLayout.LayoutParams(-2, -2);
    todayLp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
    week.addView(todayBtn, todayLp);
    week.addView(Ui.iconButton(this, R.drawable.ic_chevron_right, "下一周", true, () -> shift(1)));
    content.addView(week);

    MaterialButtonToggleGroup toggle =
        Ui.connectedGroup(
            this,
            new String[] {"周视图", "日视图", "列表"},
            new int[] {R.drawable.ic_view_week, R.drawable.ic_view_day, R.drawable.ic_view_list},
            mode,
            0,
            value ->
                content.post(
                    () -> {
                      mode = value;
                      render();
                    }));
    LinearLayout.LayoutParams tabs = new LinearLayout.LayoutParams(-1, Ui.dp(this, 48));
    tabs.setMargins(Ui.dp(this, 16), Ui.dp(this, 2), Ui.dp(this, 16), Ui.dp(this, 10));
    content.addView(toggle, tabs);
    if (snapshot.items().isEmpty()) {
      LinearLayout pad = Ui.column(this);
      pad.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 96));
      empty(
          pad,
          R.drawable.ic_school,
          "新的学期，从这里开始",
          "导入教务系统 PDF，或手动添加第一门课程。你的课表保存在本机。",
          this::importMenu,
          "导入课表",
          R.drawable.ic_upload_file);
      content.addView(Ui.scroll(this, pad), new LinearLayout.LayoutParams(-1, 0, 1));
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
    header.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), Ui.dp(this, 6));
    TextView axis =
        Ui.styled(this, "节次", com.google.android.material.R.attr.textAppearanceLabelSmall, Ui.muted(this));
    axis.setGravity(Gravity.CENTER);
    header.addView(axis, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 52)));
    LocalDate today = LocalDate.now(settings.zone());
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean isToday = date.equals(today);
      TextView h =
          Ui.text(
              this,
              Ui.day(d + 1) + "\n" + date.getDayOfMonth(),
              12,
              isToday ? Ui.onPrimary(this) : Ui.muted(this));
      h.setGravity(Gravity.CENTER);
      h.setLineSpacing(Ui.dp(this, 1), 1);
      Ui.emphasize(h, isToday ? 700 : 500);
      if (isToday) h.setBackground(Ui.roundedBg(this, Ui.primary(this), 18, 0, 0));
      h.setContentDescription(date + "，查看日课表");
      h.setOnClickListener(
          v -> {
            selected = date;
            mode = 1;
            render();
          });
      LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1);
      hp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
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
        Ui.withIcon(
            Ui.tonalButton(
                this,
                (hasNight ? "有晚间安排" : "晚间无课") + " · " + (evening ? "收起" : "展开"),
                () -> {
                  evening = !evening;
                  render();
                }),
            evening ? R.drawable.ic_expand_less : R.drawable.ic_bedtime);
    LinearLayout.LayoutParams nightLp = new LinearLayout.LayoutParams(-2, Ui.dp(this, 48));
    nightLp.setMargins(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 96), Ui.dp(this, 12));
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
      line.setAlpha(0.6f);
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
        card.setRadius(Ui.dp(this, 12));
        card.setCardBackgroundColor(Ui.courseColor(this, item.color));
        LinearLayout text = Ui.column(this);
        int pad = Ui.dp(this, 5);
        text.setPadding(pad, Ui.dp(this, 6), pad, pad);

        View accent = new View(this);
        accent.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 99, 0, 0));
        LinearLayout.LayoutParams accentLp =
            new LinearLayout.LayoutParams(Ui.dp(this, 16), Ui.dp(this, 4));
        accentLp.bottomMargin = Ui.dp(this, 4);
        text.addView(accent, accentLp);

        TextView title = Ui.text(this, item.title, 11.5f, Ui.courseInk(this, item.color));
        Ui.emphasize(title, 700);
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
    // Connected weekday selector: tight inner corners, the selected day morphs to a full pill.
    LinearLayout days = Ui.row(this);
    days.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 4));
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean active = date.equals(selected);
      MaterialCardView chip = new MaterialCardView(this);
      chip.setCardElevation(0);
      chip.setStrokeWidth(0);
      chip.setCardBackgroundColor(active ? Ui.primary(this) : Ui.surfaceContainer(this));
      float outer = 20, inner = active ? 20 : 6;
      chip.setShapeAppearanceModel(
          com.google.android.material.shape.ShapeAppearanceModel.builder()
              .setTopLeftCornerSize(Ui.dp(this, d == 0 || active ? outer : inner))
              .setBottomLeftCornerSize(Ui.dp(this, d == 0 || active ? outer : inner))
              .setTopRightCornerSize(Ui.dp(this, d == 6 || active ? outer : inner))
              .setBottomRightCornerSize(Ui.dp(this, d == 6 || active ? outer : inner))
              .build());
      LinearLayout cell = Ui.column(this);
      cell.setGravity(Gravity.CENTER);
      int fg = active ? Ui.onPrimary(this) : Ui.muted(this);
      TextView dayName = Ui.text(this, Ui.day(d + 1), 12, fg);
      dayName.setGravity(Gravity.CENTER);
      TextView dayNum = Ui.text(this, String.valueOf(date.getDayOfMonth()), 16, active ? fg : Ui.ink(this));
      dayNum.setGravity(Gravity.CENTER);
      Ui.emphasize(dayNum, 700);
      cell.addView(dayName);
      cell.addView(dayNum);
      chip.addView(cell, new FrameLayout.LayoutParams(-1, -1));
      chip.setContentDescription(date + (active ? "，已选中" : ""));
      chip.setOnClickListener(
          v -> {
            selected = date;
            render();
          });
      LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(0, Ui.dp(this, 60), 1);
      dlp.setMargins(Ui.dp(this, 1), 0, Ui.dp(this, 1), 0);
      days.addView(chip, dlp);
    }
    parent.addView(days);

    LinearLayout list = Ui.column(this);
    list.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 96));
    parent.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));
    List<Item> items = Schedule.on(snapshot.items(), selected, settings.term());
    LinearLayout headRow = Ui.row(this);
    headRow.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 6));
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
            this, items.size() + " 项安排", Ui.primaryContainer(this), Ui.onPrimaryContainer(this)));
    list.addView(headRow);
    if (items.isEmpty())
      empty(
          list,
          R.drawable.ic_event_available,
          "今天没有安排",
          "可以专注学习，也可以给自己留一点空白。",
          () -> new Editors(this).edit(null, selected, true),
          "添加日程",
          R.drawable.ic_add);
    else
      for (int i = 0; i < items.size(); i++)
        list.addView(itemCard(items.get(i), selected, i, items.size()));
  }

  void listView(LinearLayout parent) {
    LinearLayout list = Ui.column(this);
    list.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 96));
    parent.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));
    List<Item> sorted = new ArrayList<>(snapshot.items());
    sorted.sort(Comparator.comparingInt((Item i) -> i.day).thenComparingInt(i -> i.start));
    TextView summary = Ui.note(this, "全部 " + sorted.size() + " 项 · 点按编辑课程与循环规则");
    summary.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 4));
    list.addView(summary);
    // Group by weekday as an Expressive segmented list.
    int i = 0;
    while (i < sorted.size()) {
      int day = sorted.get(i).day, j = i;
      while (j < sorted.size() && sorted.get(j).day == day) j++;
      TextView header = Ui.sectionHeader(this, Ui.day(day) + " · " + (j - i) + " 项");
      header.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 8));
      list.addView(header);
      for (int k = i; k < j; k++) list.addView(itemCard(sorted.get(k), null, k - i, j - i));
      i = j;
    }
  }

  View itemCard(Item item, LocalDate date) {
    return itemCard(item, date, 0, 1);
  }

  View itemCard(Item item, LocalDate date, int position, int count) {
    int ink = Ui.courseInk(this, item.color);
    MaterialCardView card = Ui.card(this);
    card.setCardBackgroundColor(Ui.courseColor(this, item.color));
    card.setShapeAppearanceModel(
        count > 1
            ? Ui.segment(this, position, count)
            : Ui.corners(this, 24, 24));

    LinearLayout outer = Ui.row(this);
    outer.setGravity(Gravity.TOP);
    outer.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));

    // Leading time block on the course accent tone.
    LinearLayout timeBox = Ui.column(this);
    timeBox.setGravity(Gravity.CENTER);
    timeBox.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 16, 0, 0));
    timeBox.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
    int onAccent = Ui.courseColor(this, item.color);
    TextView startT = Ui.text(this, Schedule.time(item.start), 14, onAccent);
    Ui.emphasize(startT, 700);
    startT.setGravity(Gravity.CENTER);
    timeBox.addView(startT);
    TextView endT = Ui.text(this, Schedule.time(item.end), 12, onAccent);
    endT.setAlpha(0.85f);
    endT.setGravity(Gravity.CENTER);
    timeBox.addView(endT);
    LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(Ui.dp(this, 60), -2);
    timeLp.rightMargin = Ui.dp(this, 14);
    outer.addView(timeBox, timeLp);

    LinearLayout box = Ui.column(this);
    TextView meta =
        Ui.text(
            this,
            date != null
                ? Ui.day(date.getDayOfWeek().getValue())
                    + ("EVENT".equals(item.type) ? " · 日程" : " · 课程")
                : item.date.isEmpty() ? Ui.day(item.day) + " · " + item.weeks : item.date,
            12,
            ink);
    meta.setAlpha(0.8f);
    Ui.emphasize(meta, 500);
    box.addView(meta);

    TextView title = Ui.title(this, (item.done ? "✓ " : "") + item.title, ink);
    title.setTextSize(18);
    title.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 4));
    if (item.done) {
      title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
      title.setAlpha(0.7f);
    }
    box.addView(title);

    TextView sub =
        Ui.text(
            this,
            (item.location.isEmpty() ? "未设置地点" : item.location)
                + (item.teacher.isEmpty() ? "" : " · " + item.teacher)
                + (item.reminder >= 0 ? " · 提前" + item.reminder + "分钟提醒" : ""),
            13,
            ink);
    sub.setAlpha(0.8f);
    box.addView(sub);

    outer.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
    card.addView(outer);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = Ui.dp(this, count > 1 && position < count - 1 ? 2 : 10);
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
            .setIcon("EVENT".equals(item.type) ? R.drawable.ic_event : R.drawable.ic_school)
            .setTitle(item.title)
            .setMessage(content)
            .setPositiveButton(
                "编辑",
                (d, w) -> {
                  if (date != null && item.date.isEmpty())
                    new MaterialAlertDialogBuilder(this)
                        .setIcon(R.drawable.ic_edit)
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
        .setIcon(R.drawable.ic_delete)
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

  // ---- Agenda page -------------------------------------------------------------------------

  void agenda() {
    LinearLayout list = Ui.column(this);
    list.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 96));
    body.addView(Ui.scroll(this, list));

    boolean notifications = NotificationManagerCompat.from(this).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || getSystemService(AlarmManager.class).canScheduleExactAlarms();

    var events = new ArrayList<Item>();
    for (Item i : snapshot.items()) if ("EVENT".equals(i.type)) events.add(i);
    events.sort(Comparator.comparing((Item i) -> i.date).thenComparingInt(i -> i.start));
    var open = new ArrayList<Item>();
    var done = new ArrayList<Item>();
    for (Item i : events) (i.done ? done : open).add(i);

    // Hero: emphasized headline, Expressive shape mark, status chips and a medium CTA.
    MaterialCardView hero = Ui.heroCard(this);
    LinearLayout heroBox = Ui.column(this);
    heroBox.setPadding(Ui.dp(this, 24), Ui.dp(this, 22), Ui.dp(this, 24), Ui.dp(this, 22));
    LinearLayout heroTop = Ui.row(this);
    LinearLayout heroText = Ui.column(this);
    TextView hTitle = Ui.display(this, "日程与待办");
    hTitle.setTextColor(Ui.onPrimaryContainer(this));
    hTitle.setTextSize(28);
    heroText.addView(hTitle);
    TextView hCount =
        Ui.label(
            this,
            open.isEmpty() ? "暂无待完成事项" : open.size() + " 项待完成 · " + done.size() + " 项已完成",
            Ui.onPrimaryContainer(this));
    hCount.setAlpha(0.85f);
    hCount.setPadding(0, Ui.dp(this, 4), 0, 0);
    heroText.addView(hCount);
    heroTop.addView(heroText, new LinearLayout.LayoutParams(0, -2, 1));
    heroTop.addView(
        Ui.shapeIcon(
            this,
            ExpressiveShapes.SUNNY,
            R.drawable.ic_event_available,
            Ui.primary(this),
            Ui.onPrimary(this),
            72,
            32));
    heroBox.addView(heroTop);

    TextView hNote =
        Ui.text(this, "把计划放进时间里。日程会与课程一起检查冲突并提醒。", 14, Ui.onPrimaryContainer(this));
    hNote.setLineSpacing(Ui.dp(this, 2), 1);
    hNote.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
    heroBox.addView(hNote);

    LinearLayout chips = Ui.row(this);
    chips.addView(
        statusChip(notifications ? "通知已开启" : "通知未开启", R.drawable.ic_notifications, notifications, null));
    View exactChip =
        statusChip(
            exact ? "精确提醒就绪" : "近似提醒 · 点按授权",
            R.drawable.ic_alarm,
            exact,
            () -> {
              page = 2;
              render();
            });
    LinearLayout.LayoutParams ecLp = new LinearLayout.LayoutParams(-2, -2);
    ecLp.leftMargin = Ui.dp(this, 8);
    chips.addView(exactChip, ecLp);
    HorizontalScrollView chipScroll = new HorizontalScrollView(this);
    chipScroll.setHorizontalScrollBarEnabled(false);
    chipScroll.addView(chips);
    heroBox.addView(chipScroll);

    MaterialButton create =
        Ui.withIcon(
            Ui.heroButton(
                this,
                "新建日程",
                () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true)),
            R.drawable.ic_add);
    LinearLayout.LayoutParams createLp = new LinearLayout.LayoutParams(-1, -2);
    createLp.topMargin = Ui.dp(this, 10);
    heroBox.addView(create, createLp);
    hero.addView(heroBox);
    LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
    heroLp.bottomMargin = Ui.dp(this, 14);
    list.addView(hero, heroLp);

    if (!notifications || !exact) {
      list.addView(
          Ui.permissionCard(
              this,
              !notifications ? R.drawable.ic_notifications : R.drawable.ic_alarm,
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

    if (events.isEmpty()) {
      empty(
          list,
          R.drawable.ic_edit_calendar,
          "还没有日程",
          "添加自习、考试、会议或生活计划。",
          () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true),
          "安排一件事",
          R.drawable.ic_add);
      return;
    }
    if (!open.isEmpty()) {
      list.addView(agendaHeader("待完成 · " + open.size()));
      for (int i = 0; i < open.size(); i++) list.addView(eventRow(open.get(i), i, open.size()));
    }
    if (!done.isEmpty()) {
      list.addView(agendaHeader("已完成 · " + done.size()));
      for (int i = 0; i < done.size(); i++) list.addView(eventRow(done.get(i), i, done.size()));
    }
  }

  private TextView agendaHeader(String s) {
    TextView h = Ui.sectionHeader(this, s);
    h.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), 0, Ui.dp(this, 8));
    return h;
  }

  private View eventRow(Item item, int position, int count) {
    LinearLayout row = Ui.row(this);
    com.google.android.material.checkbox.MaterialCheckBox check =
        new com.google.android.material.checkbox.MaterialCheckBox(this);
    check.setChecked(item.done);
    check.setContentDescription("标记完成：" + item.title);
    check.setOnCheckedChangeListener(
        (v, isDone) -> {
          Store.Snapshot base = store.snapshot();
          for (Item i : base.items()) if (i.id.equals(item.id)) i.done = isDone;
          save(base.revision(), base.items());
        });
    row.addView(check);
    row.addView(itemCard(item, null, position, count), new LinearLayout.LayoutParams(0, -2, 1));
    return row;
  }

  private View statusChip(String text, int icon, boolean ok, Runnable action) {
    Chip chip = new Chip(this);
    chip.setText(text);
    chip.setChipIconResource(icon);
    chip.setChipIconVisible(true);
    chip.setChipBackgroundColor(
        ColorStateList.valueOf(ok ? Ui.successContainer(this) : Ui.tertiaryContainer(this)));
    int fg = ok ? Ui.onSuccessContainer(this) : Ui.onTertiaryContainer(this);
    chip.setTextColor(fg);
    chip.setChipIconTint(ColorStateList.valueOf(fg));
    chip.setChipStrokeWidth(0);
    chip.setEnsureMinTouchTargetSize(false);
    if (action != null) chip.setOnClickListener(v -> action.run());
    else chip.setClickable(false);
    return chip;
  }

  void empty(
      LinearLayout parent,
      int icon,
      String title,
      String note,
      Runnable action,
      String button,
      int buttonIcon) {
    parent.addView(Ui.emptyState(this, icon, title, note, button, buttonIcon, action));
  }
}
