package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.AlarmManager;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.*;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
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
  Boolean evening = null;

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
          granted -> refresh());

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
      if (state.containsKey("evening")) evening = state.getBoolean("evening");
    }
    monday =
        state != null
            ? LocalDate.parse(
                state.getString("monday", selected.with(DayOfWeek.MONDAY).toString()))
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
    if (evening != null) out.putBoolean("evening", evening);
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
          view.setPadding(bars.left, bars.top, bars.right, ime.bottom);
          return insets;
        });

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
        .setTitle("课表与数据")
        .setItems(
            new String[] {"从本地 PDF 导入课表", "修改课表名称", "恢复 JSON 备份", "导出 JSON 备份", "阅读本地 PDF"},
            (d, n) -> {
              switch (n) {
                case 0 -> pdfPicker.launch(new String[] {"application/pdf"});
                case 1 -> renameTable();
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

  void renameTable() {
    LinearLayout box = Ui.column(this);
    Ui.inset(box, 20);
    TextInputEditText field =
        Ui.field(box, "课表名称", settings.get("title", "青大"), false);
    new MaterialAlertDialogBuilder(this)
        .setTitle("课表名称")
        .setView(box)
        .setNegativeButton("取消", null)
        .setPositiveButton(
            "保存",
            (d, w) -> {
              String v = Ui.value(field);
              if (!v.isEmpty()) {
                settings.put("title", v);
                render();
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

  void schedule() {
    LinearLayout content = Ui.column(this);
    body.addView(content, new FrameLayout.LayoutParams(-1, -1));

    LinearLayout topBar = Ui.row(this);
    topBar.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 4));
    topBar.addView(
        Ui.circleButton(this, R.drawable.ic_layers, "课表与导入", Ui.primary(this), this::importMenu));
    TextView tableTitle = Ui.text(this, settings.get("title", "青大"), 18, Ui.ink(this));
    Ui.emphasize(tableTitle, 700);
    tableTitle.setGravity(Gravity.CENTER);
    tableTitle.setSingleLine(true);
    tableTitle.setEllipsize(TextUtils.TruncateAt.END);
    tableTitle.setOnClickListener(v -> renameTable());
    topBar.addView(tableTitle, new LinearLayout.LayoutParams(0, -2, 1));
    topBar.addView(
        Ui.circleButton(
            this,
            R.drawable.ic_add,
            "添加课程",
            Ui.primary(this),
            () -> new Editors(this).edit(null, null, false)));
    content.addView(topBar);

    LinearLayout weekBar = Ui.row(this);
    weekBar.setPadding(Ui.dp(this, 12), Ui.dp(this, 2), Ui.dp(this, 12), Ui.dp(this, 4));
    int w = Schedule.week(monday, settings.term());

    ImageView prevBtn = Ui.icon(this, R.drawable.ic_chevron_left, 24, Ui.primary(this));
    FrameLayout prevWrap = new FrameLayout(this);
    prevWrap.addView(prevBtn, new FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
    prevWrap.setContentDescription(mode == 1 ? "前一天" : "上一周");
    prevWrap.setClickable(true);
    prevWrap.setFocusable(true);
    prevWrap.setOnClickListener(v -> {
      if (mode == 1) moveDay(-1);
      else shift(-1);
    });
    weekBar.addView(prevWrap, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 36)));

    LinearLayout centerInfo = Ui.row(this);
    centerInfo.setGravity(Gravity.CENTER);
    if (mode == 1) {
      TextView dLabel =
          Ui.text(
              this,
              selected.getMonthValue()
                  + "月"
                  + selected.getDayOfMonth()
                  + "日 "
                  + Ui.day(selected.getDayOfWeek().getValue()),
              18,
              Ui.primary(this));
      Ui.emphasize(dLabel, 700);
      centerInfo.addView(dLabel);
      if (w >= 1 && w <= 30) {
        TextView wSub = Ui.text(this, "第" + w + "周", 13.5f, Ui.muted(this));
        wSub.setPadding(Ui.dp(this, 8), 0, 0, 0);
        centerInfo.addView(wSub);
      }
    } else {
      TextView wLabel =
          Ui.text(
              this,
              w < 1 ? "学期前" : w > 30 ? "学期后" : "第 " + w + " 周",
              18,
              Ui.primary(this));
      Ui.emphasize(wLabel, 700);
      centerInfo.addView(wLabel);
      TextView dSub =
          Ui.text(
              this,
              monday.getMonthValue() + "月" + monday.getDayOfMonth() + "日",
              13.5f,
              Ui.muted(this));
      dSub.setPadding(Ui.dp(this, 8), 0, 0, 0);
      centerInfo.addView(dSub);
    }
    centerInfo.setClickable(true);
    centerInfo.setFocusable(true);
    centerInfo.setOnClickListener(v -> pickWeek());
    weekBar.addView(centerInfo, new LinearLayout.LayoutParams(0, Ui.dp(this, 36), 1));

    TextView todayBtn = Ui.text(this, mode == 1 ? "本日" : "本周", 15, Ui.primary(this));
    Ui.emphasize(todayBtn, 600);
    todayBtn.setGravity(Gravity.CENTER);
    todayBtn.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
    todayBtn.setClickable(true);
    todayBtn.setFocusable(true);
    todayBtn.setOnClickListener(
        v -> {
          selected = LocalDate.now(settings.zone());
          monday = selected.with(DayOfWeek.MONDAY);
          evening = null;
          render();
        });
    weekBar.addView(todayBtn, new LinearLayout.LayoutParams(-2, Ui.dp(this, 36)));

    ImageView nextBtn = Ui.icon(this, R.drawable.ic_chevron_right, 24, Ui.primary(this));
    FrameLayout nextWrap = new FrameLayout(this);
    nextWrap.addView(nextBtn, new FrameLayout.LayoutParams(Ui.dp(this, 22), Ui.dp(this, 22), Gravity.CENTER));
    nextWrap.setContentDescription(mode == 1 ? "后一天" : "下一周");
    nextWrap.setClickable(true);
    nextWrap.setFocusable(true);
    nextWrap.setOnClickListener(v -> {
      if (mode == 1) moveDay(1);
      else shift(1);
    });
    weekBar.addView(nextWrap, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 36)));
    content.addView(weekBar);

    LinearLayout segmented =
        Ui.segmentedControl(
            this,
            new String[] {"周", "日", "列表"},
            mode,
            value -> {
              if (mode != value) {
                mode = value;
                render();
              }
            });
    LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 38));
    segLp.setMargins(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 8));
    content.addView(segmented, segLp);

    if (snapshot.items().isEmpty()) {
      LinearLayout pad = Ui.column(this);
      pad.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 96));
      empty(
          pad,
          R.drawable.ic_school,
          "当前课表为空",
          "支持导入教务系统 PDF 课表，或手动添加课程。",
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

  void pickWeek() {
    String[] items = new String[20];
    for (int i = 0; i < 20; i++) items[i] = "第 " + (i + 1) + " 周";
    new MaterialAlertDialogBuilder(this)
        .setTitle("跳转周次")
        .setItems(
            items,
            (d, which) -> {
              monday = settings.term().plusWeeks(which);
              selected = monday;
              evening = null;
              render();
            })
        .show();
  }

  void shift(int n) {
    monday = monday.plusWeeks(n);
    selected = selected.plusWeeks(n);
    evening = null;
    render();
  }

  void moveDay(int n) {
    selected = selected.plusDays(n);
    monday = selected.with(DayOfWeek.MONDAY);
    render();
  }

  void weekGrid(LinearLayout parent) {
    LinearLayout header = Ui.row(this);
    header.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 4));
    TextView axis = Ui.text(this, "节次", 11, Ui.muted(this));
    axis.setGravity(Gravity.CENTER);
    header.addView(axis, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 50)));

    LocalDate today = LocalDate.now(settings.zone());
    boolean todayInWeek = !today.isBefore(monday) && !today.isAfter(monday.plusDays(6));
    LocalDate highlightDate = todayInWeek ? today : selected;

    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean active = date.equals(highlightDate);
      LinearLayout cell = Ui.column(this);
      cell.setGravity(Gravity.CENTER);
      TextView dayName = Ui.text(this, Ui.day(d + 1), 11.5f, Ui.muted(this));
      dayName.setGravity(Gravity.CENTER);
      cell.addView(dayName);

      TextView dayNum =
          Ui.text(
              this,
              String.valueOf(date.getDayOfMonth()),
              14,
              active ? Ui.onPrimary(this) : Ui.ink(this));
      dayNum.setGravity(Gravity.CENTER);
      Ui.emphasize(dayNum, 700);
      if (active) dayNum.setBackground(Ui.roundedBg(this, Ui.primary(this), 999, 0, 0));
      LinearLayout.LayoutParams numLp =
          new LinearLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28));
      numLp.topMargin = Ui.dp(this, 4);
      cell.addView(dayNum, numLp);

      cell.setContentDescription(date + "，查看日课表");
      cell.setClickable(true);
      cell.setFocusable(true);
      cell.setOnClickListener(
          v -> {
            selected = date;
            mode = 1;
            render();
          });
      header.addView(cell, new LinearLayout.LayoutParams(0, Ui.dp(this, 52), 1));
    }
    parent.addView(header);

    boolean hasNight = false;
    for (int d = 0; d < 7; d++)
      if (Schedule.on(snapshot.items(), monday.plusDays(d), settings.term()).stream()
          .anyMatch(i -> i.start >= 1080)) hasNight = true;
    boolean showEvening = evening != null ? evening : hasNight;
    int count = showEvening ? 10 : 8;
    int axisW = Ui.dp(this, 40);
    int rowH = Ui.dp(this, 60);
    int breakH = 0;
    int totalH = periodTop(count, rowH, breakH);

    ScrollView vertical = new ScrollView(this);
    vertical.setVerticalScrollBarEnabled(false);
    parent.addView(vertical, new LinearLayout.LayoutParams(-1, 0, 1));

    LinearLayout scrollBody = Ui.column(this);
    vertical.addView(scrollBody, new ScrollView.LayoutParams(-1, -2));

    FrameLayout gridStage = new FrameLayout(this);
    scrollBody.addView(gridStage, new LinearLayout.LayoutParams(-1, totalH));

    int[] starts = settings.starts(), ends = settings.ends();
    for (int i = 0; i < count; i++) {
      int y = periodTop(i, rowH, breakH);
      LinearLayout timeCell = Ui.column(this);
      timeCell.setGravity(Gravity.CENTER);
      TextView pNum = Ui.text(this, String.valueOf(i + 1), 11.5f, Ui.ink(this));
      Ui.emphasize(pNum, 700);
      pNum.setGravity(Gravity.CENTER);
      TextView pStart = Ui.text(this, Schedule.time(starts[i]), 9f, Ui.muted(this));
      pStart.setGravity(Gravity.CENTER);
      TextView pEnd = Ui.text(this, Schedule.time(ends[i]), 9f, Ui.muted(this));
      pEnd.setAlpha(0.75f);
      pEnd.setGravity(Gravity.CENTER);
      timeCell.addView(pNum);
      timeCell.addView(pStart);
      timeCell.addView(pEnd);
      FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(axisW, rowH);
      tp.topMargin = y;
      gridStage.addView(timeCell, tp);

      View line = new View(this);
      line.setBackgroundColor(Ui.outlineVariant(this));
      line.setAlpha(0.45f);
      FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, 1);
      lp.leftMargin = axisW;
      lp.topMargin = y;
      gridStage.addView(line, lp);
    }

    LinearLayout columnsRow = Ui.row(this);
    columnsRow.setGravity(Gravity.TOP);
    FrameLayout.LayoutParams colsLp = new FrameLayout.LayoutParams(-1, totalH);
    colsLp.leftMargin = axisW;
    gridStage.addView(columnsRow, colsLp);

    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      FrameLayout dayCol = new FrameLayout(this);
      if (date.equals(highlightDate)) {
        dayCol.setBackgroundColor(Ui.isDark(this) ? 0x0C4D94FF : 0x0A1A66E8);
      }
      columnsRow.addView(dayCol, new LinearLayout.LayoutParams(0, totalH, 1));

      List<Item> dayItems = new ArrayList<>();
      for (Item item : Schedule.on(snapshot.items(), date, settings.term())) {
        if (!showEvening && item.start >= 1080) continue;
        if (position(item.start, starts, ends) < count) dayItems.add(item);
      }
      List<List<Item>> groups = overlapGroups(dayItems);
      for (List<Item> group : groups) {
        if (group.size() == 1) {
          Item item = group.get(0);
          int topPx = minuteToY(item.start, starts, ends, count, rowH, breakH);
          int bottomPx = minuteToY(item.end, starts, ends, count, rowH, breakH);
          View card = buildGridCourseCard(item, date, false);
          FrameLayout.LayoutParams cp =
              new FrameLayout.LayoutParams(
                  -1, Math.max(Ui.dp(this, 38), bottomPx - topPx - Ui.dp(this, 4)));
          cp.topMargin = topPx + Ui.dp(this, 2);
          cp.leftMargin = Ui.dp(this, 2);
          cp.rightMargin = Ui.dp(this, 2);
          dayCol.addView(card, cp);
        } else {
          int groupStart = group.stream().mapToInt(i -> i.start).min().orElse(510);
          int groupEnd = group.stream().mapToInt(i -> i.end).max().orElse(610);
          int topPx = minuteToY(groupStart, starts, ends, count, rowH, breakH);
          int bottomPx = minuteToY(groupEnd, starts, ends, count, rowH, breakH);
          LinearLayout laneRow = Ui.row(this);
          laneRow.setGravity(Gravity.TOP);
          for (Item item : group) {
            View card = buildGridCourseCard(item, date, true);
            LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                    0, Math.max(Ui.dp(this, 38), bottomPx - topPx - Ui.dp(this, 4)), 1);
            lp.leftMargin = Ui.dp(this, 1);
            lp.rightMargin = Ui.dp(this, 1);
            laneRow.addView(card, lp);
          }
          FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, -2);
          rp.topMargin = topPx + Ui.dp(this, 2);
          dayCol.addView(laneRow, rp);
        }
      }
    }

    LinearLayout footer = Ui.row(this);
    footer.setGravity(Gravity.CENTER);
    footer.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
    MaterialButton nightBtn =
        Ui.withIcon(
            Ui.tonalButton(
                this,
                (hasNight ? "晚间有课" : "晚间无课") + " · " + (showEvening ? "收起" : "展开"),
                () -> {
                  evening = !showEvening;
                  render();
                }),
            showEvening ? R.drawable.ic_expand_less : R.drawable.ic_bedtime);
    nightBtn.setMinHeight(0);
    nightBtn.setMinimumHeight(0);
    nightBtn.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
    footer.addView(nightBtn, new LinearLayout.LayoutParams(-2, -2));
    scrollBody.addView(footer);
  }

  private View buildGridCourseCard(Item item, LocalDate date, boolean conflict) {
    MaterialCardView card = Ui.card(this);
    card.setRadius(Ui.dp(this, 9));
    card.setCardBackgroundColor(Ui.courseColor(this, item.color));
    LinearLayout text = Ui.column(this);
    int pad = Ui.dp(this, 4);
    text.setPadding(pad, Ui.dp(this, 4), pad, pad);

    View accent = new View(this);
    accent.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 99, 0, 0));
    LinearLayout.LayoutParams accentLp =
        new LinearLayout.LayoutParams(-1, Ui.dp(this, 3));
    accentLp.leftMargin = Ui.dp(this, 2);
    accentLp.rightMargin = Ui.dp(this, 2);
    accentLp.bottomMargin = Ui.dp(this, 3);
    text.addView(accent, accentLp);

    TextView title = Ui.text(this, item.title, 10.5f, Ui.courseInk(this, item.color));
    Ui.emphasize(title, 700);
    title.setMaxLines(3);
    title.setEllipsize(TextUtils.TruncateAt.END);
    title.setLineSpacing(0, 0.96f);
    text.addView(title);

    if (!item.location.isEmpty()) {
      TextView location =
          Ui.text(
              this,
              item.location.replace('_', ' '),
              9f,
              Ui.courseInk(this, item.color));
      location.setAlpha(0.78f);
      location.setMaxLines(3);
      location.setEllipsize(TextUtils.TruncateAt.END);
      location.setLineSpacing(0, 0.95f);
      location.setPadding(0, Ui.dp(this, 2), 0, 0);
      text.addView(location);
    }
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
            + (conflict ? "，有时间冲突" : ""));
    card.setOnClickListener(v -> details(item, date));
    return card;
  }

  private static List<List<Item>> overlapGroups(List<Item> items) {
    List<List<Item>> groups = new ArrayList<>();
    for (Item item : items) {
      if (groups.isEmpty()) {
        List<Item> g = new ArrayList<>();
        g.add(item);
        groups.add(g);
      } else {
        List<Item> last = groups.get(groups.size() - 1);
        boolean overlaps = last.stream().anyMatch(other -> Schedule.overlaps(other, item));
        if (overlaps) last.add(item);
        else {
          List<Item> g = new ArrayList<>();
          g.add(item);
          groups.add(g);
        }
      }
    }
    return groups;
  }

  private static int periodTop(int periodIndex, int rowH, int breakH) {
    int gaps = (periodIndex > 4 ? 1 : 0) + (periodIndex > 8 ? 1 : 0);
    return periodIndex * rowH + gaps * breakH;
  }

  private static int minuteToY(
      int minute, int[] starts, int[] ends, int count, int rowH, int breakH) {
    float pos = Math.min(count, position(minute, starts, ends));
    int idx = Math.min(count - 1, Math.max(0, (int) Math.floor(pos)));
    float frac = pos - idx;
    if (pos >= count) return periodTop(count, rowH, breakH);
    return (int) (periodTop(idx, rowH, breakH) + frac * rowH);
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
    days.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 6));
    for (int d = 0; d < 7; d++) {
      LocalDate date = monday.plusDays(d);
      boolean active = date.equals(selected);
      LinearLayout cell = Ui.column(this);
      cell.setGravity(Gravity.CENTER);
      TextView dayName = Ui.text(this, Ui.day(d + 1), 11.5f, Ui.muted(this));
      dayName.setGravity(Gravity.CENTER);
      TextView dayNum =
          Ui.text(
              this,
              String.valueOf(date.getDayOfMonth()),
              14.5f,
              active ? Ui.onPrimary(this) : Ui.ink(this));
      dayNum.setGravity(Gravity.CENTER);
      Ui.emphasize(dayNum, 700);
      if (active) dayNum.setBackground(Ui.roundedBg(this, Ui.primary(this), 999, 0, 0));
      LinearLayout.LayoutParams numLp =
          new LinearLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30));
      numLp.topMargin = Ui.dp(this, 4);
      cell.addView(dayName);
      cell.addView(dayNum, numLp);
      cell.setContentDescription(date + (active ? "，已选中" : ""));
      cell.setClickable(true);
      cell.setFocusable(true);
      cell.setOnClickListener(
          v -> {
            selected = date;
            render();
          });
      days.addView(cell, new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1));
    }
    parent.addView(days);

    LinearLayout list = Ui.column(this);
    list.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 96));
    parent.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));
    List<Item> items = Schedule.on(snapshot.items(), selected, settings.term());
    LinearLayout headRow = Ui.row(this);
    headRow.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 8));
    headRow.addView(
        Ui.subheading(
            this,
            selected.getMonthValue()
                + "月"
                + selected.getDayOfMonth()
                + "日  "
                + Ui.day(selected.getDayOfWeek().getValue())),
        new LinearLayout.LayoutParams(0, -2, 1));
    headRow.addView(
        Ui.pillBadge(
            this, items.size() + " 项", Ui.primaryContainer(this), Ui.onPrimaryContainer(this)));
    list.addView(headRow);
    if (items.isEmpty())
      empty(
          list,
          R.drawable.ic_event_available,
          "当天暂无课程与日程",
          "点按下方按钮添加当天安排。",
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
    int i = 0;
    while (i < sorted.size()) {
      int day = sorted.get(i).day, j = i;
      while (j < sorted.size() && sorted.get(j).day == day) j++;
      TextView header = Ui.sectionHeader(this, Ui.day(day) + " · " + (j - i) + " 项");
      header.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), 0, Ui.dp(this, 6));
      list.addView(header);
      for (int k = i; k < j; k++) list.addView(itemCard(sorted.get(k), null, k - i, j - i));
      i = j;
    }
  }

  View itemCard(Item item, LocalDate date, int position, int count) {
    int ink = Ui.courseInk(this, item.color);
    MaterialCardView card = Ui.card(this);
    card.setCardBackgroundColor(Ui.courseColor(this, item.color));
    card.setShapeAppearanceModel(
        count > 1 ? Ui.segment(this, position, count) : Ui.corners(this, 20, 20));

    LinearLayout outer = Ui.row(this);
    outer.setGravity(Gravity.CENTER_VERTICAL);
    outer.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));

    LinearLayout timeBox = Ui.column(this);
    timeBox.setGravity(Gravity.CENTER);
    timeBox.setBackground(Ui.roundedBg(this, Ui.courseAccent(this, item.color), 14, 0, 0));
    timeBox.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
    int onAccent = Ui.courseColor(this, item.color);
    TextView startT = Ui.text(this, Schedule.time(item.start), 13.5f, onAccent);
    Ui.emphasize(startT, 700);
    startT.setGravity(Gravity.CENTER);
    timeBox.addView(startT);
    TextView endT = Ui.text(this, Schedule.time(item.end), 11.5f, onAccent);
    endT.setAlpha(0.88f);
    endT.setGravity(Gravity.CENTER);
    timeBox.addView(endT);
    LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(Ui.dp(this, 58), -2);
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
            11.5f,
            ink);
    meta.setAlpha(0.78f);
    Ui.emphasize(meta, 500);
    box.addView(meta);

    TextView title = Ui.title(this, (item.done ? "✓ " : "") + item.title, ink);
    title.setTextSize(16.5f);
    title.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 3));
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
            12.5f,
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
        .setNeutralButton("删除", (d, w) -> remove(item, date))
        .show();
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
              review(
                  "确认删除",
                  base,
                  next,
                  "删除后可通过已有的 JSON 备份恢复。",
                  () -> save(base.revision(), next));
            })
        .setNegativeButton("取消", null)
        .show();
  }

  void agenda() {
    LinearLayout outer = Ui.column(this);
    body.addView(outer, new FrameLayout.LayoutParams(-1, -1));

    LinearLayout topBar = Ui.row(this);
    topBar.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 6));
    TextView pageTitle = Ui.display(this, "日程");
    pageTitle.setTextSize(26);
    topBar.addView(pageTitle, new LinearLayout.LayoutParams(0, -2, 1));
    topBar.addView(
        Ui.circleButton(
            this,
            R.drawable.ic_add,
            "新建日程",
            Ui.primary(this),
            () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true)));
    outer.addView(topBar);

    LinearLayout list = Ui.column(this);
    list.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 96));
    outer.addView(Ui.scroll(this, list), new LinearLayout.LayoutParams(-1, 0, 1));

    boolean notifications = NotificationManagerCompat.from(this).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || getSystemService(AlarmManager.class).canScheduleExactAlarms();

    if (!notifications || !exact) {
      MaterialCardView permBanner =
          Ui.listItem(
              this,
              !notifications ? R.drawable.ic_notifications : R.drawable.ic_alarm,
              !notifications ? "通知权限未开启" : "精确提醒权限待开启",
              !notifications ? "开启后可在课前与日程到点时收到横幅提醒" : "开启后可在省电与锁屏下准点唤醒提醒",
              Ui.filledButton(
                  this,
                  "开启",
                  () -> {
                    if (!notifications && Build.VERSION.SDK_INT >= 33)
                      notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
                    else new SettingsScreen(this).requestExactAlarmPermission();
                  }),
              0,
              1,
              null);
      LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
      bp.bottomMargin = Ui.dp(this, 12);
      permBanner.setLayoutParams(bp);
      list.addView(permBanner);
    }

    var events = new ArrayList<Item>();
    for (Item i : snapshot.items()) if ("EVENT".equals(i.type)) events.add(i);
    events.sort(Comparator.comparing((Item i) -> i.date).thenComparingInt(i -> i.start));
    var open = new ArrayList<Item>();
    var done = new ArrayList<Item>();
    for (Item i : events) (i.done ? done : open).add(i);

    if (events.isEmpty()) {
      empty(
          list,
          R.drawable.ic_edit_calendar,
          "暂无日程安排",
          "可添加考试、自习或待办事项，与课表共用冲突检查和提醒。",
          () -> new Editors(this).edit(null, LocalDate.now(settings.zone()), true),
          "新建日程",
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
    h.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), 0, Ui.dp(this, 8));
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
