package com.ddyec.yiclass.ui;

import android.net.Uri;
import android.widget.*;
import com.ddyec.yiclass.ai.AssistantClient;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.ddyec.yiclass.importer.PdfImporter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

final class ImportFlow {
  private final MainActivity a;

  ImportFlow(MainActivity a) {
    this.a = a;
  }

  void pdf(Uri uri) {
    var loading =
        new MaterialAlertDialogBuilder(a)
            .setTitle("正在读取本地 PDF")
            .setMessage("识别课程、星期、节次和单双周…")
            .setCancelable(false)
            .show();
    a.worker.execute(
        () -> {
          try {
            var result = new PdfImporter().read(a, uri);
            int[] starts = a.settings.starts(), ends = a.settings.ends();
            for (Item item : result.items()) {
              for (int i = 0; i < 10; i++) {
                if (item.start == Schedule.STARTS[i]) {
                  item.start = starts[i];
                  break;
                }
              }
              for (int i = 0; i < 10; i++) {
                if (item.end == Schedule.ENDS[i]) {
                  item.end = ends[i];
                  break;
                }
              }
              item.validate();
            }
            a.runOnUiThread(
                () -> {
                  loading.dismiss();
                  if (!a.isDestroyed()) previewPdf(uri, result);
                });
          } catch (Exception e) {
            a.runOnUiThread(
                () -> {
                  loading.dismiss();
                  a.error(e);
                });
          }
        });
  }

  void previewPdf(Uri uri, PdfTableParser.Result result) {
    Store.Snapshot base = a.store.snapshot();
    Set<String> fingerprints = new HashSet<>();
    for (Item i : base.items()) fingerprints.add(i.fingerprint());
    List<Item> fresh = new ArrayList<>();
    for (Item i : result.items()) if (!fingerprints.contains(i.fingerprint())) fresh.add(i);
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 18);
    box.addView(
        Ui.note(
            a,
            "识别 "
                + result.items().size()
                + " 条安排，跳过 "
                + (result.items().size() - fresh.size())
                + " 条重复项。可取消不需要导入的课程。\n\n"
                + String.join("\n", result.warnings())));
    TextInputEditText term = Ui.field(box, "第1周的周一（务必核对）", a.settings.term().toString(), false);
    box.addView(Ui.button(a, "查看原始 PDF", () -> new PdfPreview(a).show(uri)));
    List<android.widget.CheckBox> selections = new ArrayList<>();
    for (Item item : fresh) {
      var c = new com.google.android.material.checkbox.MaterialCheckBox(a);
      c.setChecked(true);
      c.setText(
          item.title
              + "\n"
              + Ui.day(item.day)
              + "  "
              + item.weeks
              + "  "
              + Schedule.time(item.start)
              + "–"
              + Schedule.time(item.end)
              + "\n"
              + item.location
              + " · "
              + item.teacher);
      c.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
      selections.add(c);
      box.addView(c);
    }
    var dialog =
        new MaterialAlertDialogBuilder(a)
            .setTitle("核对导入结果")
            .setView(Ui.scroll(a, box))
            .setNegativeButton("取消", null)
            .setPositiveButton("导入所选", null)
            .create();
    dialog.setOnShowListener(
        v ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    w -> {
                      try {
                        LocalDate first = LocalDate.parse(Ui.value(term));
                        if (first.getDayOfWeek() != DayOfWeek.MONDAY)
                          throw new IllegalArgumentException("学期首周日期必须是星期一");
                        List<Item> next = new ArrayList<>(base.items());
                        int added = 0;
                        for (int i = 0; i < fresh.size(); i++)
                          if (selections.get(i).isChecked()) {
                            next.add(fresh.get(i));
                            added++;
                          }
                        if (added == 0) throw new IllegalArgumentException("没有选择新课程");
                        List<String> conflicts = Schedule.conflicts(next, first);
                        new MaterialAlertDialogBuilder(a)
                            .setTitle("导入 " + added + " 条安排？")
                            .setMessage(
                                "学期首周："
                                    + first
                                    + "\n修改首周日期将同时影响已有的循环课程。\n\n"
                                    + (conflicts.isEmpty()
                                        ? "未发现时间冲突。"
                                        : String.join("\n", conflicts)))
                            .setNegativeButton("返回核对", null)
                            .setPositiveButton(
                                "确认导入",
                                (d, n) -> {
                                  try {
                                    a.store.replace(base.revision(), next);
                                    a.settings.put("term", first.toString());
                                    a.refresh();
                                    dialog.dismiss();
                                    a.toast("课表已导入");
                                  } catch (Exception e) {
                                    a.error(e);
                                  }
                                })
                            .show();
                      } catch (Exception e) {
                        a.error(e);
                      }
                    }));
    dialog.show();
  }

  void export(Uri uri) {
    String json = Backup.export(a.store.snapshot(), a.settings);
    a.worker.execute(
        () -> {
          try (OutputStream out = a.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("无法写入所选文件");
            out.write(json.getBytes(StandardCharsets.UTF_8));
            a.runOnUiThread(() -> a.toast("备份已导出，不包含 API Key"));
          } catch (Exception e) {
            a.runOnUiThread(() -> a.error(e));
          }
        });
  }

  void restore(Uri uri) {
    a.worker.execute(
        () -> {
          try {
            String json =
                AssistantClient.readBounded(
                    a.getContentResolver().openInputStream(uri), 10 * 1024 * 1024);
            Backup.Decoded backup = Backup.decode(json);
            a.runOnUiThread(
                () -> {
                  Store.Snapshot base = a.store.snapshot();
                  new MaterialAlertDialogBuilder(a)
                      .setTitle("恢复备份")
                      .setMessage(
                          "将用备份中的 "
                              + backup.items().size()
                              + " 项安排替换当前课表。\n首周："
                              + backup.term()
                              + "\n时区："
                              + backup.zone()
                              + "\n建议先导出当前课表。此操作不修改 AI Key。")
                      .setNegativeButton("取消", null)
                      .setPositiveButton(
                          "替换并恢复",
                          (d, w) -> {
                            try {
                              a.store.replace(base.revision(), backup.items());
                              a.settings.put("term", backup.term());
                              a.settings.put("zone", backup.zone());
                              a.settings.put("starts", backup.starts());
                              a.settings.put("ends", backup.ends());
                              a.refresh();
                            } catch (Exception e) {
                              a.error(e);
                            }
                          })
                      .show();
                });
          } catch (Exception e) {
            a.runOnUiThread(() -> a.error(e));
          }
        });
  }
}
