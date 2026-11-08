package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.widget.AgendaWidget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    Ui.inset(box, 20);

    MaterialCardView hero = Ui.heroCard(a);
    LinearLayout heroBody = Ui.column(a);
    Ui.inset(heroBody, 20);
    heroBody.addView(
        Ui.pillBadge(
            a,
            "MATERIAL 3 EXPRESSIVE · 本机优先",
            Ui.surfaceLow(a),
            Ui.primary(a)));
    TextView heroTitle = Ui.text(a, "设置与权限中心", 24, Ui.onPrimaryContainer(a));
    heroTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    heroTitle.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 4));
    heroBody.addView(heroTitle);
    TextView heroNote =
        Ui.text(
            a,
            "YI课表 · 原生 Android\n课程保存在本机，可随时导出备份；提醒与权限状态一目了然。",
            13,
            Ui.onPrimaryContainer(a));
    heroNote.setLineSpacing(Ui.dp(a, 3), 1);
    heroBody.addView(heroNote);
    hero.addView(heroBody);
    LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(-1, -2);
    heroLp.bottomMargin = Ui.dp(a, 8);
    box.addView(hero, heroLp);

    boolean notifications = NotificationManagerCompat.from(a).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || a.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    boolean allReady = notifications && exact;

    sectionWithBadge(
        box,
        "权限与桌面组件",
        allReady ? "权限全部就绪" : "待完善权限",
        allReady);

    box.addView(
        Ui.permissionCard(
            a,
            "🔔",
            "课程与日程通知权限",
            "通知："
                + (notifications ? "已开启" : "未开启")
                + " · 在每项安排开始前，按你单独设定的提前量发送系统横幅与锁屏提醒。",
            notifications ? "已开启" : "未开启",
            notifications,
            "设置通知权限",
            () -> requestNotificationPermission(notifications)));

    if (Build.VERSION.SDK_INT >= 31) {
      box.addView(
          Ui.permissionCard(
              a,
              "⏱",
              "精确时间唤醒权限",
              "精确提醒："
                  + (exact ? "可用" : "未授权，将使用系统允许的近似提醒")
                  + " · 保障低电量或息屏状态下准点唤醒；设备重启、时区或系统时间变更后自动恢复。",
              exact ? "精准可用" : "近似模式",
              exact,
              "允许精确提醒",
              this::requestExactAlarmPermission));
    }

    box.addView(
        Ui.permissionCard(
            a,
            "⊞",
            "桌面课表小组件",
            "在手机主屏幕固定可调整大小的今日与未来七天近期课表卡片，点按直达主界面。",
            "桌面组件",
            true,
            "添加桌面课表组件",
            () -> {
              AppWidgetManager manager = AppWidgetManager.getInstance(a);
              if (manager.isRequestPinAppWidgetSupported())
                manager.requestPinAppWidget(new ComponentName(a, AgendaWidget.class), null, null);
              else a.toast("请长按桌面，在小组件中选择 YI课表");
            }));

    box.addView(
        Ui.button(a, "查看权限用途与隐私边界说明", () -> showPermissionGuide(notifications, exact)));

    section(box, "学期与显示");
    box.addView(
        Ui.settingTile(
            a,
            "学期首周 · " + a.settings.term(),
            a.settings.term().toString(),
            "第 1 周星期一日期，用于推算全部循环周次",
            this::term));
    box.addView(
        Ui.settingTile(
            a,
            "时区 · " + a.settings.zone(),
            a.settings.zone().getId(),
            "课表钟点与午夜自动刷新所使用的 IANA 时区",
            this::zone));
    box.addView(
        Ui.settingTile(
            a,
            "作息时间 · 10节课",
            "10 节",
            "自定义第 1–10 节起止时间，用于周视图刻度与 PDF 导入",
            this::slots));
    String themeLabel =
        a.settings.theme() == -1 ? "跟随系统" : a.settings.theme() == 2 ? "深色" : "浅色";
    box.addView(
        Ui.settingTile(
            a,
            "外观 · " + themeLabel,
            themeLabel,
            "切换浅色、深色或跟随系统昼夜模式",
            () ->
                new MaterialAlertDialogBuilder(a)
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

    section(box, "AI助手");
    MaterialCardView aiCard = Ui.surfaceCard(a);
    LinearLayout aiBody = Ui.column(a);
    Ui.inset(aiBody, 18);
    LinearLayout aiHeader = Ui.row(a);
    TextView aiTitle = Ui.subheading(a, "智能时间与调课助手");
    aiHeader.addView(aiTitle, new LinearLayout.LayoutParams(0, -2, 1));
    aiHeader.addView(
        Ui.pillBadge(
            a,
            a.settings.get("model", "deepseek-chat"),
            Ui.primaryContainer(a),
            Ui.onPrimaryContainer(a)));
    aiBody.addView(aiHeader);
    aiBody.addView(
        Ui.note(
            a,
            "默认使用 DeepSeek 兼容接口。仅在你发送请求时，将课程内容发往所配置的服务；原 PDF 不会上传。Key 由 Android Keystore"
                + " 加密保存，不包含在备份中。"));
    aiBody.addView(Ui.tonalButton(a, "配置接口、模型与 API Key", this::api));
    aiCard.addView(aiBody);
    box.addView(aiCard);

    section(box, "数据与备份");
    LinearLayout dataCol = Ui.column(a);
    MaterialButton importBtn =
        Ui.filledButton(a, "从 PDF 导入", () -> a.pdfPicker.launch(new String[] {"application/pdf"}));
    LinearLayout.LayoutParams btnGap = new LinearLayout.LayoutParams(-1, Ui.dp(a, 48));
    btnGap.bottomMargin = Ui.dp(a, 8);
    dataCol.addView(importBtn, btnGap);

    LinearLayout backupRow = Ui.row(a);
    MaterialButton exportBtn =
        Ui.tonalButton(
            a, "导出完整课表备份", () -> a.backupWriter.launch("YI课表-" + LocalDate.now() + ".json"));
    LinearLayout.LayoutParams halfLeft = new LinearLayout.LayoutParams(0, Ui.dp(a, 48), 1);
    halfLeft.rightMargin = Ui.dp(a, 6);
    backupRow.addView(exportBtn, halfLeft);

    MaterialButton restoreBtn =
        Ui.button(
            a,
            "恢复备份",
            () ->
                a.backupPicker.launch(
                    new String[] {"application/json", "text/plain", "application/octet-stream"}));
    LinearLayout.LayoutParams halfRight = new LinearLayout.LayoutParams(0, Ui.dp(a, 48), 1);
    halfRight.leftMargin = Ui.dp(a, 6);
    backupRow.addView(restoreBtn, halfRight);
    dataCol.addView(backupRow);
    box.addView(dataCol);

    TextView footer =
        Ui.note(a, "版本 1.0.0 · Java / Material 3 Expressive\n图标沿用 YIClass；课程核心与构建独立重写。");
    footer.setGravity(Gravity.CENTER_HORIZONTAL);
    footer.setPadding(0, Ui.dp(a, 18), 0, Ui.dp(a, 12));
    box.addView(footer);
    return Ui.scroll(a, box);
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
        .setTitle("权限与隐私说明")
        .setView(Ui.scroll(a, box))
        .setPositiveButton("知道了", null)
        .setNeutralButton(
            notifications ? "管理通知" : "开启通知",
            (d, w) -> requestNotificationPermission(notifications))
        .show();
  }

  private void section(LinearLayout box, String title) {
    TextView h = Ui.text(a, title, 16, Ui.primary(a));
    h.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    h.setPadding(Ui.dp(a, 4), Ui.dp(a, 20), 0, Ui.dp(a, 10));
    box.addView(h);
  }

  private void sectionWithBadge(LinearLayout box, String title, String badge, boolean active) {
    LinearLayout row = Ui.row(a);
    row.setPadding(Ui.dp(a, 4), Ui.dp(a, 18), Ui.dp(a, 4), Ui.dp(a, 10));
    TextView h = Ui.text(a, title, 16, Ui.primary(a));
    h.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    row.addView(h, new LinearLayout.LayoutParams(0, -2, 1));
    row.addView(Ui.statusBadge(a, badge, active));
    box.addView(row);
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
