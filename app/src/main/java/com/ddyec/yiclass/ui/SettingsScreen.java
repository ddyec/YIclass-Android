package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.widget.AgendaLargeWidget;
import com.ddyec.yiclass.widget.AgendaMediumWidget;
import com.ddyec.yiclass.widget.AgendaWidget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import java.time.*;
import java.util.*;

final class SettingsScreen {
  private final MainActivity a;

  SettingsScreen(MainActivity a) {
    this.a = a;
  }

  View build() {
    LinearLayout outer = Ui.column(a);

    LinearLayout topBar = Ui.row(a);
    topBar.setPadding(Ui.dp(a, 20), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 6));
    TextView pageTitle = Ui.display(a, "设置");
    pageTitle.setTextSize(26);
    topBar.addView(pageTitle);
    outer.addView(topBar);

    LinearLayout box = Ui.column(a);
    box.setPadding(Ui.dp(a, 16), Ui.dp(a, 4), Ui.dp(a, 16), Ui.dp(a, 96));

    boolean notifications = NotificationManagerCompat.from(a).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || a.getSystemService(AlarmManager.class).canScheduleExactAlarms();

    MaterialCardView brandCard = Ui.surfaceCard(a);
    brandCard.setCardBackgroundColor(Ui.primaryContainer(a));
    LinearLayout brandRow = Ui.row(a);
    brandRow.setPadding(Ui.dp(a, 18), Ui.dp(a, 16), Ui.dp(a, 18), Ui.dp(a, 16));
    MaterialCardView iconWrap = new MaterialCardView(a);
    iconWrap.setRadius(Ui.dp(a, 14));
    iconWrap.setCardElevation(0);
    iconWrap.setStrokeWidth(0);
    ImageView logoImg = new ImageView(a);
    logoImg.setImageResource(R.drawable.brand_logo);
    logoImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
    iconWrap.addView(logoImg, new FrameLayout.LayoutParams(Ui.dp(a, 52), Ui.dp(a, 52)));
    LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(Ui.dp(a, 52), Ui.dp(a, 52));
    logoLp.rightMargin = Ui.dp(a, 14);
    brandRow.addView(iconWrap, logoLp);

    LinearLayout brandText = Ui.column(a);
    TextView appName = Ui.title(a, "YIClass", Ui.onPrimaryContainer(a));
    appName.setTextSize(19);
    brandText.addView(appName);
    long courseCount =
        a.snapshot.items().stream().filter(i -> !"EVENT".equals(i.type)).count();
    TextView summary =
        Ui.text(
            a,
            "当前课表："
                + a.settings.get("title", "青大")
                + " · "
                + courseCount
                + " 项课程",
            13,
            Ui.onPrimaryContainer(a));
    summary.setAlpha(0.85f);
    summary.setPadding(0, Ui.dp(a, 2), 0, 0);
    brandText.addView(summary);
    brandRow.addView(brandText, new LinearLayout.LayoutParams(0, -2, 1));
    brandCard.addView(brandRow);
    box.addView(brandCard, Ui.wrap(a, 0, 4));

    section(box, "课表与时间");
    String themeLabel =
        a.settings.theme() == -1 ? "跟随系统" : a.settings.theme() == 2 ? "深色" : "浅色";
    List<View> display = new ArrayList<>();
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_edit,
            "课表名称",
            "显示在课表页顶部的标题",
            Ui.trailingValue(a, a.settings.get("title", "青大")),
            0,
            1,
            a::renameTable));
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_school,
            "学期首周",
            "日历选择或输入日期，非周一自动对齐至当周周一",
            Ui.trailingValue(a, a.settings.term().toString()),
            0,
            1,
            this::term));
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_schedule,
            "作息时间",
            "滚轮拖动调整第 1–10 节起止时间",
            Ui.trailingValue(a, "10 节"),
            0,
            1,
            this::slots));
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_public,
            "时区",
            "课表钟点与午夜自动刷新使用的 IANA 时区",
            Ui.trailingValue(a, a.settings.zone().getId()),
            0,
            1,
            this::zone));
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_bedtime,
            "外观",
            "浅色、深色或跟随系统",
            Ui.trailingValue(a, themeLabel),
            0,
            1,
            () ->
                new MaterialAlertDialogBuilder(a)
                    .setIcon(R.drawable.ic_bedtime)
                    .setTitle("外观")
                    .setSingleChoiceItems(
                        new String[] {"跟随系统", "浅色", "深色"},
                        a.settings.theme() == -1 ? 0 : a.settings.theme() == 2 ? 2 : 1,
                        (d, n) -> {
                          int value = new int[] {-1, 1, 2}[n];
                          a.settings.put("theme", String.valueOf(value));
                          AppCompatDelegate.setDefaultNightMode(value);
                          d.dismiss();
                        })
                    .show()));
    if (DynamicColors.isDynamicColorAvailable()) {
      MaterialSwitch dynamic = new MaterialSwitch(a);
      dynamic.setChecked(a.settings.dynamicColor());
      dynamic.setContentDescription("动态配色");
      dynamic.setOnCheckedChangeListener(
          (v, on) -> {
            a.settings.put("dynamic_color", on ? "1" : "0");
            a.recreate();
          });
      display.add(
          Ui.listItem(
              a,
              R.drawable.ic_palette,
              "动态配色",
              "跟随壁纸取色（Material You）",
              dynamic,
              0,
              1,
              dynamic::toggle));
    }
    addGroup(box, display);

    section(box, "提醒与小组件");
    List<View> perm = new ArrayList<>();
    perm.add(
        permissionItem(
            R.drawable.ic_notifications,
            "课程与日程通知",
            notifications ? "按每项安排的提前量发送提醒" : "未开启 · 到点时无法弹出提醒",
            notifications,
            () -> requestNotificationPermission(notifications)));
    if (Build.VERSION.SDK_INT >= 31)
      perm.add(
          permissionItem(
              R.drawable.ic_alarm,
              "精确时间唤醒",
              exact ? "锁屏与省电模式下准点提醒" : "近似模式 · 授权后可准点唤醒",
              exact,
              this::requestExactAlarmPermission));
    perm.add(
        Ui.listItem(
            a,
            R.drawable.ic_widgets,
            "桌面课表小组件",
            "提供 2×2 / 4×2 / 4×3 三种尺寸并支持自由缩放",
            Ui.icon(a, R.drawable.ic_add, 22, Ui.primary(a)),
            0,
            1,
            this::pinWidget));
    MaterialSwitch debugTomorrowSwitch = new MaterialSwitch(a);
    debugTomorrowSwitch.setChecked(a.settings.widgetDebugTomorrow());
    debugTomorrowSwitch.setContentDescription("强制展示明日课程");
    debugTomorrowSwitch.setOnCheckedChangeListener(
        (v, on) -> {
          a.settings.put("widget_debug_tomorrow", on ? "1" : "0");
          AgendaWidget.refreshAll(a);
          a.toast(on ? "调试开启：小组件已强制展示明日课程" : "调试关闭：小组件已恢复按时间正常显示");
        });
    perm.add(
        Ui.listItem(
            a,
            R.drawable.ic_auto_awesome,
            "小组件调试：强制展示明日课程",
            "忽略时间限制直接展示明日多节课程，用于检查容器排版与溢出",
            debugTomorrowSwitch,
            0,
            1,
            debugTomorrowSwitch::toggle));
    perm.add(
        Ui.listItem(
            a,
            R.drawable.ic_privacy_tip,
            "权限与隐私说明",
            "最小权限原则，不扫描存储、通讯录或定位",
            chevron(),
            0,
            1,
            () -> showPermissionGuide(notifications, exact)));
    addGroup(box, perm);

    section(box, "AI 助手（DeepSeek 官方接口）");
    List<View> ai = new ArrayList<>();
    ai.add(
        Ui.listItem(
            a,
            R.drawable.ic_book,
            "DeepSeek 模型与 API Key",
            "官方最新 DeepSeek-V4.1 Flash (deepseek-flash) 与 DeepSeek-V4 Pro (deepseek-v4-pro)",
            Ui.trailingValue(a, a.settings.model()),
            0,
            1,
            this::api));
    ai.add(
        Ui.listItem(
            a,
            R.drawable.ic_key,
            "数据与密钥保护",
            "支持直接读写本地 JSON 课表并展示操作摘要；API Key 由 Android Keystore 加密存储",
            null,
            0,
            1,
            null));
    addGroup(box, ai);

    section(box, "数据与备份");
    List<View> data = new ArrayList<>();
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_upload_file,
            "从本地 PDF 导入",
            "本地识别教务课表，核对后导入",
            chevron(),
            0,
            1,
            () -> a.pdfPicker.launch(new String[] {"application/pdf"})));
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_download,
            "导出 JSON 备份",
            "包含课程、学期、时区与作息，不含 API Key",
            chevron(),
            0,
            1,
            () -> a.backupWriter.launch("YIClass-" + LocalDate.now() + ".json")));
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_restore,
            "恢复 JSON 备份",
            "用备份文件替换当前课表",
            chevron(),
            0,
            1,
            () ->
                a.backupPicker.launch(
                    new String[] {"application/json", "text/plain", "application/octet-stream"})));
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_pdf,
            "阅读本地 PDF",
            "在应用内查看原始课表文件",
            chevron(),
            0,
            1,
            () -> a.pdfViewer.launch(new String[] {"application/pdf"})));
    addGroup(box, data);

    outer.addView(Ui.scroll(a, box), new LinearLayout.LayoutParams(-1, 0, 1));
    return outer;
  }

  private void pinWidget() {
    String[] sizes = {
      "小号 (2×2) · 紧凑卡片（支持自由缩放）",
      "中号 (4×2) · 今日待上列表（支持自由缩放）",
      "大号 (4×3) · 完整今日课表（支持自由缩放）"
    };
    Class<?>[] classes = {
      AgendaWidget.class, AgendaMediumWidget.class, AgendaLargeWidget.class
    };
    new MaterialAlertDialogBuilder(a)
        .setIcon(R.drawable.ic_widgets)
        .setTitle("添加桌面小组件")
        .setItems(
            sizes,
            (d, which) -> {
              AppWidgetManager manager = AppWidgetManager.getInstance(a);
              if (manager.isRequestPinAppWidgetSupported()) {
                manager.requestPinAppWidget(new ComponentName(a, classes[which]), null, null);
              } else {
                a.toast("请长按手机桌面，在「小组件」中选择 YIClass 对应尺寸");
              }
            })
        .show();
  }

  private View chevron() {
    return Ui.icon(a, R.drawable.ic_chevron_right, 22, Ui.muted(a));
  }

  private View permissionItem(
      int icon, String title, String supporting, boolean granted, Runnable action) {
    View trailing =
        granted ? Ui.statusBadge(a, "已开启", true) : Ui.filledButton(a, "开启", action);
    return Ui.listItem(a, icon, title, supporting, trailing, 0, 1, action);
  }

  private void addGroup(LinearLayout box, List<View> items) {
    for (int i = 0; i < items.size(); i++) {
      View v = items.get(i);
      if (v instanceof MaterialCardView card) {
        card.setShapeAppearanceModel(Ui.segment(a, i, items.size()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = Ui.dp(a, i == items.size() - 1 ? 0 : 2);
        card.setLayoutParams(lp);
      }
      box.addView(v);
    }
  }

  void requestNotificationPermission(boolean currentlyEnabled) {
    if (Build.VERSION.SDK_INT >= 33 && !currentlyEnabled)
      a.notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    else
      a.startActivity(
          new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
              .putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName()));
  }

  void requestExactAlarmPermission() {
    if (Build.VERSION.SDK_INT >= 31) {
      try {
        a.startActivity(
            new Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + a.getPackageName())));
      } catch (ActivityNotFoundException e) {
        a.error(e);
      }
    } else {
      a.toast("当前 Android 版本已默认支持精确闹钟");
    }
  }

  void showPermissionGuide(boolean notifications, boolean exact) {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    box.addView(
        Ui.note(
            a,
            "• 通知权限（"
                + (notifications ? "已开启" : "未开启")
                + "）：用于课前与日程到点横幅提醒。\n"
                + "• 精确提醒权限（"
                + (exact ? "已开启" : "近似模式")
                + "）：用于锁屏与省电模式下准点唤醒。\n"
                + "• 开机广播恢复：重启或修改时区后自动重排提醒。\n"
                + "• 本地文件访问：通过系统文件选择器单次读取所选 PDF 或备份文件。"));
    new MaterialAlertDialogBuilder(a)
        .setIcon(R.drawable.ic_privacy_tip)
        .setTitle("权限与隐私说明")
        .setView(Ui.scroll(a, box))
        .setPositiveButton("知道了", null)
        .setNeutralButton(
            notifications ? "管理通知" : "开启通知",
            (d, w) -> requestNotificationPermission(notifications))
        .show();
  }

  private void section(LinearLayout box, String title) {
    TextView h = Ui.sectionHeader(a, title);
    h.setPadding(Ui.dp(a, 6), Ui.dp(a, 18), 0, Ui.dp(a, 8));
    box.addView(h);
  }

  void term() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 16);
    LocalDate currentTerm = a.settings.term();
    TextInputEditText field =
        Ui.field(box, "输入日期（如 09-03 或 2026-09-03）", currentTerm.toString(), false);
    TextView driftHint =
        Ui.text(
            a,
            "当周周一：" + currentTerm + "（选择任意日期会自动漂移至当周周一）",
            12.5f,
            Ui.primary(a));
    Ui.emphasize(driftHint, 600);
    driftHint.setPadding(Ui.dp(a, 4), Ui.dp(a, 2), Ui.dp(a, 4), Ui.dp(a, 8));
    box.addView(driftHint);

    CalendarView calendarView = new CalendarView(a);
    calendarView.setFirstDayOfWeek(Calendar.MONDAY);
    calendarView.setDate(
        currentTerm.atStartOfDay(a.settings.zone()).toInstant().toEpochMilli(), false, true);
    calendarView.setOnDateChangeListener(
        (view, year, month, dayOfMonth) -> {
          LocalDate picked = LocalDate.of(year, month + 1, dayOfMonth);
          LocalDate monday = picked.with(DayOfWeek.MONDAY);
          field.setText(monday.toString());
          driftHint.setText(
              picked.equals(monday)
                  ? "已选周一：" + monday
                  : "已选 " + picked + " → 自动漂移至当周周一：" + monday);
        });
    box.addView(calendarView, new LinearLayout.LayoutParams(-1, Ui.dp(a, 300)));

    field.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            try {
              LocalDate monday =
                  Schedule.parseTermMonday(s.toString(), currentTerm.getYear());
              driftHint.setText("将自动对齐至当周周一：" + monday);
            } catch (Exception ignored) {
            }
          }
        });

    showForm(
        "设置学期首周",
        box,
        () -> {
          LocalDate monday =
              Schedule.parseTermMonday(Ui.value(field), currentTerm.getYear());
          a.settings.put("term", monday.toString());
          a.refresh();
          a.toast("学期首周已设为周一：" + monday);
        });
  }

  void zone() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    TextInputEditText field =
        Ui.field(box, "IANA 时区，例如 Asia/Shanghai", a.settings.zone().getId(), false);
    showForm(
        "课表时区",
        box,
        () -> {
          ZoneId zone = ZoneId.of(Ui.value(field));
          a.settings.put("zone", zone.getId());
          a.refresh();
        });
  }

  void slots() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 16);
    int[] starts = a.settings.starts().clone();
    int[] ends = a.settings.ends().clone();
    MaterialButton[] startBtns = new MaterialButton[10];
    MaterialButton[] endBtns = new MaterialButton[10];

    LinearLayout topRow = Ui.row(a);
    TextView tip = Ui.note(a, "点击任意时间，滑动滚轮即可调节（默认每节 45 分钟）。");
    topRow.addView(tip, new LinearLayout.LayoutParams(0, -2, 1));
    MaterialButton resetBtn =
        Ui.tonalButton(
            a,
            "恢复默认",
            () -> {
              for (int i = 0; i < 10; i++) {
                starts[i] = Schedule.STARTS[i];
                ends[i] = Schedule.ENDS[i];
                startBtns[i].setText(Schedule.time(starts[i]));
                endBtns[i].setText(Schedule.time(ends[i]));
              }
            });
    resetBtn.setMinHeight(0);
    resetBtn.setMinimumHeight(0);
    resetBtn.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
    topRow.addView(resetBtn);
    box.addView(topRow, Ui.wrap(a, 0, 10));

    for (int i = 0; i < 10; i++) {
      final int idx = i;
      MaterialCardView rowCard = Ui.surfaceCard(a);
      rowCard.setShapeAppearanceModel(Ui.segment(a, i, 10));
      LinearLayout row = Ui.row(a);
      row.setPadding(Ui.dp(a, 16), Ui.dp(a, 10), Ui.dp(a, 14), Ui.dp(a, 10));

      TextView label = Ui.title(a, "第 " + (i + 1) + " 节", Ui.ink(a));
      label.setTextSize(15);
      row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

      MaterialButton sBtn =
          Ui.tonalButton(
              a,
              Schedule.time(starts[i]),
              () ->
                  pickWheelTime(
                      "第 " + (idx + 1) + " 节 · 开始时间",
                      starts[idx],
                      newMin -> {
                        int duration = Math.max(30, ends[idx] - starts[idx]);
                        starts[idx] = newMin;
                        startBtns[idx].setText(Schedule.time(starts[idx]));
                        if (ends[idx] <= starts[idx] || duration == 45) {
                          ends[idx] = Math.min(1439, starts[idx] + duration);
                          endBtns[idx].setText(Schedule.time(ends[idx]));
                        }
                      }));
      sBtn.setMinHeight(0);
      sBtn.setMinimumHeight(0);
      sBtn.setPadding(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 6));
      startBtns[i] = sBtn;
      row.addView(sBtn);

      TextView dash = Ui.text(a, " — ", 14, Ui.muted(a));
      dash.setPadding(Ui.dp(a, 6), 0, Ui.dp(a, 6), 0);
      row.addView(dash);

      MaterialButton eBtn =
          Ui.tonalButton(
              a,
              Schedule.time(ends[i]),
              () ->
                  pickWheelTime(
                      "第 " + (idx + 1) + " 节 · 结束时间",
                      ends[idx],
                      newMin -> {
                        ends[idx] = newMin;
                        endBtns[idx].setText(Schedule.time(ends[idx]));
                      }));
      eBtn.setMinHeight(0);
      eBtn.setMinimumHeight(0);
      eBtn.setPadding(Ui.dp(a, 14), Ui.dp(a, 6), Ui.dp(a, 14), Ui.dp(a, 6));
      endBtns[i] = eBtn;
      row.addView(eBtn);

      rowCard.addView(row);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = Ui.dp(a, i == 9 ? 0 : 2);
      box.addView(rowCard, lp);
    }

    showForm(
        "学校作息时间",
        box,
        () -> {
          List<String> ss = new ArrayList<>(), es = new ArrayList<>();
          for (int i = 0; i < 10; i++) {
            ss.add(String.valueOf(starts[i]));
            es.add(String.valueOf(ends[i]));
          }
          String start = String.join(",", ss), end = String.join(",", es);
          Backup.validateSlots(start, end);
          a.settings.put("starts", start);
          a.settings.put("ends", end);
          a.refresh();
          a.toast("作息时间已保存");
        });
  }

  private void pickWheelTime(
      String title, int initialMinute, java.util.function.IntConsumer onPicked) {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    LinearLayout wheels = Ui.row(a);
    wheels.setGravity(Gravity.CENTER);

    NumberPicker hourPicker = new NumberPicker(a);
    hourPicker.setMinValue(0);
    hourPicker.setMaxValue(23);
    hourPicker.setValue(Math.max(0, Math.min(23, initialMinute / 60)));
    hourPicker.setFormatter(v -> String.format(Locale.ROOT, "%02d 时", v));

    TextView colon = Ui.title(a, ":", Ui.ink(a));
    colon.setTextSize(22);
    colon.setPadding(Ui.dp(a, 16), 0, Ui.dp(a, 16), 0);

    NumberPicker minPicker = new NumberPicker(a);
    minPicker.setMinValue(0);
    minPicker.setMaxValue(59);
    minPicker.setValue(Math.max(0, Math.min(59, initialMinute % 60)));
    minPicker.setFormatter(v -> String.format(Locale.ROOT, "%02d 分", v));

    wheels.addView(hourPicker, new LinearLayout.LayoutParams(Ui.dp(a, 110), -2));
    wheels.addView(colon);
    wheels.addView(minPicker, new LinearLayout.LayoutParams(Ui.dp(a, 110), -2));
    box.addView(wheels);

    new MaterialAlertDialogBuilder(a)
        .setTitle(title)
        .setView(box)
        .setNegativeButton("取消", null)
        .setPositiveButton(
            "确定",
            (d, w) -> {
              hourPicker.clearFocus();
              minPicker.clearFocus();
              onPicked.accept(hourPicker.getValue() * 60 + minPicker.getValue());
            })
        .show();
  }

  void api() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    TextInputEditText endpoint =
        Ui.field(
            box,
            "API 地址（官方：https://api.deepseek.com）",
            a.settings.get("endpoint", "https://api.deepseek.com"),
            false);
    TextInputEditText model =
        Ui.field(
            box,
            "模型名称（deepseek-flash 或 deepseek-v4-pro）",
            a.settings.model(),
            false);

    LinearLayout presetRow = Ui.row(a);
    MaterialButton flashBtn =
        Ui.tonalButton(
            a,
            "V4.1 Flash (deepseek-flash)",
            () -> {
              endpoint.setText("https://api.deepseek.com");
              model.setText("deepseek-flash");
            });
    flashBtn.setMinHeight(0);
    flashBtn.setMinimumHeight(0);
    flashBtn.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 10), Ui.dp(a, 6));
    MaterialButton proBtn =
        Ui.tonalButton(
            a,
            "V4 Pro (deepseek-v4-pro)",
            () -> {
              endpoint.setText("https://api.deepseek.com");
              model.setText("deepseek-v4-pro");
            });
    proBtn.setMinHeight(0);
    proBtn.setMinimumHeight(0);
    proBtn.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 10), Ui.dp(a, 6));
    LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, -2, 1);
    p1.rightMargin = Ui.dp(a, 6);
    LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, -2, 1);
    p2.leftMargin = Ui.dp(a, 6);
    presetRow.addView(flashBtn, p1);
    presetRow.addView(proBtn, p2);
    box.addView(presetRow, Ui.wrap(a, 0, 8));

    com.google.android.material.checkbox.MaterialCheckBox thinkingBox =
        new com.google.android.material.checkbox.MaterialCheckBox(a);
    thinkingBox.setText("开启深度思考模式（thinking: enabled，支持 V4.1 Flash 与 V4 Pro）");
    thinkingBox.setChecked(a.settings.thinking());
    box.addView(thinkingBox);

    TextInputEditText key = Ui.field(box, "DeepSeek API Key（sk-...，留空保留已存密钥）", "", false);
    key.setInputType(129);
    key.setSaveEnabled(false);
    com.google.android.material.checkbox.MaterialCheckBox clear =
        new com.google.android.material.checkbox.MaterialCheckBox(a);
    clear.setText("清除本机已保存的 API Key");
    box.addView(clear);
    box.addView(
        Ui.note(
            a,
            "已严格遵循 DeepSeek V4.1 / V4 官方接口规范：默认使用 DeepSeek-V4.1-Flash-1017 (deepseek-flash)，支持 DeepSeek-V4-Pro-0813 (deepseek-v4-pro)、thinking 思考控制与 JSON 输出模式。"));
    showForm(
        "配置 DeepSeek AI 助手",
        box,
        () -> {
          String rawUrl = Ui.value(endpoint).replaceAll("/+$", "");
          if (rawUrl.endsWith("/chat/completions")) {
            rawUrl = rawUrl.substring(0, rawUrl.length() - "/chat/completions".length());
          }
          java.net.URI uri = java.net.URI.create(rawUrl);
          if (!"https".equals(uri.getScheme())
              || uri.getHost() == null
              || uri.getUserInfo() != null
              || uri.getQuery() != null
              || uri.getFragment() != null) throw new IllegalArgumentException("请输入有效的 HTTPS 地址");
          if (Ui.value(model).isEmpty()) throw new IllegalArgumentException("请填写模型名称");
          if (clear.isChecked()) new SecretStore(a).save("");
          else if (!Ui.value(key).isEmpty()) new SecretStore(a).save(Ui.value(key));
          a.settings.put("endpoint", rawUrl);
          a.settings.put("model", Ui.value(model));
          a.settings.put("thinking", thinkingBox.isChecked() ? "1" : "0");
          a.render();
          a.toast("AI 服务配置已保存");
        });
  }

  private interface Save {
    void run() throws Exception;
  }

  private void showForm(String title, LinearLayout box, Save save) {
    var dialog =
        new MaterialAlertDialogBuilder(a)
            .setTitle(title)
            .setView(Ui.scroll(a, box))
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create();
    dialog.setOnShowListener(
        d ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      try {
                        save.run();
                        dialog.dismiss();
                      } catch (Exception e) {
                        a.error(e);
                      }
                    }));
    dialog.show();
  }
}
