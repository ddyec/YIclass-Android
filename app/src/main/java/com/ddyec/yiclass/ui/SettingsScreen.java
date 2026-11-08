package com.ddyec.yiclass.ui;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.widget.AgendaWidget;
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
    Ui.inset(box, 22);
    box.addView(Ui.heading(a, "设置"));
    box.addView(Ui.note(a, "YI课表 · 原生 Android\n课程保存在本机，可随时导出备份。"));
    section(box, "学期与显示");
    box.addView(Ui.button(a, "学期首周 · " + a.settings.term(), this::term));
    box.addView(Ui.button(a, "时区 · " + a.settings.zone(), this::zone));
    box.addView(Ui.button(a, "作息时间 · 10节课", this::slots));
    box.addView(
        Ui.button(
            a,
            "外观 · " + (a.settings.theme() == -1 ? "跟随系统" : a.settings.theme() == 2 ? "深色" : "浅色"),
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
    section(box, "提醒与桌面组件");
    boolean notifications = NotificationManagerCompat.from(a).areNotificationsEnabled();
    boolean exact =
        Build.VERSION.SDK_INT < 31
            || a.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    box.addView(
        Ui.note(
            a,
            "通知："
                + (notifications ? "已开启" : "未开启")
                + "\n精确提醒："
                + (exact ? "可用" : "未授权，将使用系统允许的近似提醒")
                + "\n每项安排可单独设置提前量；设备重启、时区或系统时间变更后自动恢复。"));
    box.addView(
        Ui.button(
            a,
            "设置通知权限",
            () -> {
              if (Build.VERSION.SDK_INT >= 33 && !notifications)
                a.notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
              else
                a.startActivity(
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName()));
            }));
    if (Build.VERSION.SDK_INT >= 31)
      box.addView(
          Ui.button(
              a,
              "允许精确提醒",
              () -> {
                try {
                  a.startActivity(
                      new Intent(
                          Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + a.getPackageName())));
                } catch (ActivityNotFoundException e) {
                  a.error(e);
                }
              }));
    box.addView(
        Ui.button(
            a,
            "添加桌面课表组件",
            () -> {
              AppWidgetManager manager = AppWidgetManager.getInstance(a);
              if (manager.isRequestPinAppWidgetSupported())
                manager.requestPinAppWidget(new ComponentName(a, AgendaWidget.class), null, null);
              else a.toast("请长按桌面，在小组件中选择 YI课表");
            }));
    section(box, "AI助手");
    box.addView(
        Ui.note(
            a,
            "默认使用 DeepSeek 兼容接口。仅在你发送请求时，将课程内容发往所配置的服务；原 PDF 不会上传。Key 由 Android Keystore"
                + " 加密保存，不包含在备份中。"));
    box.addView(Ui.button(a, "配置接口、模型与 API Key", this::api));
    section(box, "数据与备份");
    box.addView(
        Ui.button(a, "从 PDF 导入", () -> a.pdfPicker.launch(new String[] {"application/pdf"})));
    box.addView(
        Ui.button(a, "导出完整课表备份", () -> a.backupWriter.launch("YI课表-" + LocalDate.now() + ".json")));
    box.addView(
        Ui.button(
            a,
            "恢复备份",
            () ->
                a.backupPicker.launch(
                    new String[] {"application/json", "text/plain", "application/octet-stream"})));
    box.addView(Ui.note(a, "版本 1.0.0 · Java / Material 3\n图标沿用 YIClass；课程核心与构建独立重写。"));
    return Ui.scroll(a, box);
  }

  private void section(LinearLayout box, String title) {
    TextView h = Ui.text(a, title, 16, Ui.primary(a));
    h.setTypeface(null, android.graphics.Typeface.BOLD);
    h.setPadding(0, Ui.dp(a, 22), 0, Ui.dp(a, 8));
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
