package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.widget.AgendaWidget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.time.*;
import java.util.*;

final class SettingsScreen {
  private final MainActivity a;

  SettingsScreen(MainActivity a) {
    this.a = a;
  }

  View build() {
    LinearLayout box = Ui.column(a);
    box.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 96));

    boolean notifications = NotificationManagerCompat.from(a).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || a.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    boolean allReady = notifications && exact;
    int pending = (notifications ? 0 : 1) + (exact ? 0 : 1);

    // Hero: emphasized page title with an Expressive shape reflecting permission health.
    MaterialCardView hero = Ui.heroCard(a);
    if (!allReady) hero.setCardBackgroundColor(Ui.tertiaryContainer(a));
    int heroInk = allReady ? Ui.onPrimaryContainer(a) : Ui.onTertiaryContainer(a);
    LinearLayout heroBody = Ui.row(a);
    heroBody.setPadding(Ui.dp(a, 24), Ui.dp(a, 22), Ui.dp(a, 20), Ui.dp(a, 22));
    LinearLayout heroText = Ui.column(a);
    TextView heroTitle = Ui.display(a, "设置");
    heroTitle.setTextColor(heroInk);
    heroText.addView(heroTitle);
    TextView heroState =
        Ui.label(a, allReady ? "提醒权限全部就绪" : "还有 " + pending + " 项提醒权限待开启", heroInk);
    heroState.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 6));
    heroText.addView(heroState);
    TextView heroNote = Ui.text(a, "课程保存在本机，可随时导出备份。", 13, heroInk);
    heroNote.setAlpha(0.85f);
    heroText.addView(heroNote);
    heroBody.addView(heroText, new LinearLayout.LayoutParams(0, -2, 1));
    heroBody.addView(
        Ui.shapeIcon(
            a,
            allReady ? ExpressiveShapes.COOKIE_9 : ExpressiveShapes.SOFT_BURST,
            allReady ? R.drawable.ic_check : R.drawable.ic_notifications,
            allReady ? Ui.primary(a) : Ui.tertiary(a),
            allReady
                ? Ui.onPrimary(a)
                : Ui.color(a, com.google.android.material.R.attr.colorOnTertiary),
            76,
            34));
    hero.addView(heroBody);
    box.addView(hero, Ui.wrap(a, 0, 4));

    // Permissions & widget — segmented list.
    section(box, "提醒与权限");
    List<View> perm = new ArrayList<>();
    perm.add(
        permissionItem(
            R.drawable.ic_notifications,
            "课程与日程通知",
            notifications ? "按每项安排的提前量发送横幅与锁屏提醒" : "未开启 · 到点时无法弹出提醒",
            notifications,
            () -> requestNotificationPermission(notifications)));
    if (Build.VERSION.SDK_INT >= 31)
      perm.add(
          permissionItem(
              R.drawable.ic_alarm,
              "精确时间唤醒",
              exact ? "低电量或息屏时也能准点提醒" : "近似模式 · 授权后可准点唤醒",
              exact,
              this::requestExactAlarmPermission));
    perm.add(
        Ui.listItem(
            a,
            R.drawable.ic_widgets,
            "桌面课表小组件",
            "固定今日与未来七天的近期安排",
            Ui.icon(a, R.drawable.ic_add, 24, Ui.muted(a)),
            0,
            1,
            () -> {
              AppWidgetManager manager = AppWidgetManager.getInstance(a);
              if (manager.isRequestPinAppWidgetSupported())
                manager.requestPinAppWidget(new ComponentName(a, AgendaWidget.class), null, null);
              else a.toast("请长按桌面，在小组件中选择 YI课表");
            }));
    perm.add(
        Ui.listItem(
            a,
            R.drawable.ic_privacy_tip,
            "权限用途与隐私边界",
            "最小权限原则，不扫描存储、通讯录或定位",
            chevron(),
            0,
            1,
            () -> showPermissionGuide(notifications, exact)));
    addGroup(box, perm);

    // Semester & display.
    section(box, "学期与显示");
    String themeLabel =
        a.settings.theme() == -1 ? "跟随系统" : a.settings.theme() == 2 ? "深色" : "浅色";
    List<View> display = new ArrayList<>();
    display.add(
        Ui.listItem(
            a,
            R.drawable.ic_school,
            "学期首周",
            "第 1 周星期一，用于推算全部循环周次",
            Ui.trailingValue(a, a.settings.term().toString()),
            0,
            1,
            this::term));
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
            R.drawable.ic_schedule,
            "作息时间",
            "自定义第 1–10 节起止时间",
            Ui.trailingValue(a, "10 节"),
            0,
            1,
            this::slots));
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

    // AI assistant.
    section(box, "AI 助手");
    List<View> ai = new ArrayList<>();
    ai.add(
        Ui.listItem(
            a,
            R.drawable.ic_auto_awesome,
            "接口、模型与 API Key",
            "默认 DeepSeek 兼容接口",
            Ui.trailingValue(a, a.settings.get("model", "deepseek-chat")),
            0,
            1,
            this::api));
    ai.add(
        Ui.listItem(
            a,
            R.drawable.ic_key,
            "数据与密钥",
            "仅在发送请求时提交课程内容，原 PDF 不上传；Key 由 Android Keystore 加密且不进入备份",
            null,
            0,
            1,
            null));
    addGroup(box, ai);

    // Data & backup.
    section(box, "数据与备份");
    List<View> data = new ArrayList<>();
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_upload_file,
            "从 PDF 导入",
            "本地识别教务课表，逐项核对后导入",
            chevron(),
            0,
            1,
            () -> a.pdfPicker.launch(new String[] {"application/pdf"})));
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_download,
            "导出完整课表备份",
            "JSON，包含学期、时区与作息，不含 API Key",
            chevron(),
            0,
            1,
            () -> a.backupWriter.launch("YI课表-" + LocalDate.now() + ".json")));
    data.add(
        Ui.listItem(
            a,
            R.drawable.ic_restore,
            "恢复备份",
            "用备份替换当前课表",
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
            "在应用内查看原始课表页面",
            chevron(),
            0,
            1,
            () -> a.pdfViewer.launch(new String[] {"application/pdf"})));
    addGroup(box, data);

    TextView footer =
        Ui.note(a, "版本 1.0.0 · Java / Material 3 Expressive\n图标沿用 YIClass；课程核心与构建独立重写。");
    footer.setGravity(Gravity.CENTER_HORIZONTAL);
    footer.setPadding(0, Ui.dp(a, 24), 0, Ui.dp(a, 12));
    box.addView(footer);
    return Ui.scroll(a, box);
  }

  private View chevron() {
    return Ui.icon(a, R.drawable.ic_chevron_right, 24, Ui.muted(a));
  }

  private View permissionItem(
      int icon, String title, String supporting, boolean granted, Runnable action) {
    View trailing =
        granted
            ? Ui.statusBadge(a, "已开启", true)
            : Ui.filledButton(a, "开启", action);
    return Ui.listItem(a, icon, title, supporting, trailing, 0, 1, action);
  }

  /** Re-applies segmented shapes once the group size is known. */
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
            "YI课表遵循最小权限原则，不申请存储空间扫描、通讯录或定位权限：\n\n"
                + "• 通知权限（"
                + (notifications ? "已开启" : "未开启")
                + "）：用于课前与日程到点横幅提醒。\n"
                + "• 精确提醒权限（"
                + (exact ? "已开启" : "近似模式")
                + "）：用于准点唤醒，未开启时回退系统节能近似提醒。\n"
                + "• 开机广播恢复：重启或修改时区后自动重排下一次提醒，无常驻后台服务。\n"
                + "• 本地文件访问：通过系统文件选择器单次读取你选中的 PDF 或备份文件。"));
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
    h.setPadding(Ui.dp(a, 8), Ui.dp(a, 22), 0, Ui.dp(a, 10));
    box.addView(h);
  }

  void term() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    TextInputEditText field =
        Ui.field(box, "第1周周一 YYYY-MM-DD", a.settings.term().toString(), false);
    box.addView(Ui.note(a, "修改后所有循环课程、提醒和组件会按新首周计算。"));
    showForm(
        "学期首周",
        box,
        () -> {
          LocalDate d = LocalDate.parse(Ui.value(field));
          if (d.getDayOfWeek() != DayOfWeek.MONDAY) throw new IllegalArgumentException("请填写星期一的日期");
          a.settings.put("term", d.toString());
          a.refresh();
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
    Ui.inset(box, 18);
    box.addView(Ui.note(a, "作息用于周视图刻度与以后 PDF 的节次换算。已有课程保留各自钟点，如需更改请编辑课程。"));
    List<TextInputEditText> fields = new ArrayList<>();
    int[] starts = a.settings.starts(), ends = a.settings.ends();
    for (int i = 0; i < 10; i++)
      fields.add(
          Ui.field(
              box,
              "第" + (i + 1) + "节 HH:mm-HH:mm",
              Schedule.time(starts[i]) + "-" + Schedule.time(ends[i]),
              false));
    showForm(
        "学校作息",
        box,
        () -> {
          List<String> ss = new ArrayList<>(), es = new ArrayList<>();
          for (var field : fields) {
            String[] p = Ui.value(field).split("-");
            if (p.length != 2) throw new IllegalArgumentException("作息格式为 HH:mm-HH:mm");
            ss.add(String.valueOf(Schedule.minute(p[0])));
            es.add(String.valueOf(Schedule.minute(p[1])));
          }
          String start = String.join(",", ss), end = String.join(",", es);
          Backup.validateSlots(start, end);
          a.settings.put("starts", start);
          a.settings.put("ends", end);
          a.refresh();
        });
  }

  void api() {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 20);
    TextInputEditText endpoint =
        Ui.field(box, "HTTPS 基础地址", a.settings.get("endpoint", "https://api.deepseek.com"), false);
    TextInputEditText model =
        Ui.field(box, "模型 ID", a.settings.get("model", "deepseek-chat"), false);
    TextInputEditText key = Ui.field(box, "API Key（留空保留已有值）", "", false);
    key.setInputType(129);
    key.setSaveEnabled(false);
    com.google.android.material.checkbox.MaterialCheckBox clear =
        new com.google.android.material.checkbox.MaterialCheckBox(a);
    clear.setText("删除本机已保存的 Key");
    box.addView(clear);
    box.addView(Ui.note(a, "填基础地址，不要附加 /chat/completions。第三方接口请自行核对可信性。"));
    showForm(
        "AI服务配置",
        box,
        () -> {
          java.net.URI uri = java.net.URI.create(Ui.value(endpoint));
          if (!"https".equals(uri.getScheme())
              || uri.getHost() == null
              || uri.getUserInfo() != null
              || uri.getQuery() != null
              || uri.getFragment() != null) throw new IllegalArgumentException("请输入有效的HTTPS基础地址");
          if (Ui.value(model).isEmpty()) throw new IllegalArgumentException("请填写模型ID");
          if (clear.isChecked()) new SecretStore(a).save("");
          else if (!Ui.value(key).isEmpty()) new SecretStore(a).save(Ui.value(key));
          a.settings.put("endpoint", Ui.value(endpoint));
          a.settings.put("model", Ui.value(model));
          a.toast("AI配置已保存");
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
