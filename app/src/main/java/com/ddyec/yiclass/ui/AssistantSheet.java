package com.ddyec.yiclass.ui;

import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.ai.AssistantClient;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.google.android.material.bottomsheet.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.loadingindicator.LoadingIndicator;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

final class AssistantSheet {
  private final MainActivity a;
  private final List<AssistantClient.Turn> history = new ArrayList<>();

  AssistantSheet(MainActivity a) {
    this.a = a;
  }

  void show() {
    BottomSheetDialog dialog = new BottomSheetDialog(a);
    LinearLayout box = Ui.column(a);
    box.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 24));
    box.addView(Ui.sheetHandle(a));

    LinearLayout topRow = Ui.row(a);
    topRow.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 10));
    ImageView mark = Ui.icon(a, R.drawable.ic_book, 32, Ui.primary(a));
    topRow.addView(mark);
    LinearLayout titleCol = Ui.column(a);
    titleCol.setPadding(Ui.dp(a, 10), 0, 0, 0);
    TextView headTitle = Ui.heading(a, "AI 课表助手");
    headTitle.setTextSize(20);
    titleCol.addView(headTitle);
    TextView subTitle =
        Ui.text(
            a,
            "模型："
                + a.settings.model()
                + (a.settings.thinking() ? " (思考)" : "")
                + " · 支持直接修改与写入课表 JSON",
            12,
            Ui.muted(a));
    titleCol.addView(subTitle);
    topRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
    MaterialButton jsonBtn =
        Ui.tonalButton(a, "课表 JSON", () -> showJsonEditor(dialog));
    jsonBtn.setMinHeight(0);
    jsonBtn.setMinimumHeight(0);
    jsonBtn.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
    topRow.addView(jsonBtn);
    box.addView(topRow);

    TextInputEditText input =
        Ui.field(box, "输入你的课表调整需求，AI 将直接执行并写入 JSON…", "", true);
    input.setMinLines(3);
    input.setMaxLines(6);
    Ui.startIcon(input, R.drawable.ic_edit_calendar);

    ChipGroup examples = new ChipGroup(a);
    examples.setChipSpacingVertical(Ui.dp(a, 4));
    for (String text :
        new String[] {
          "检查本周是否有时间冲突",
          "在明天下午两点安排一小时自习",
          "把周三的篮球课调到周五第5-6节",
          "删除本周所有已完成的日程"
        }) {
      Chip chip = new Chip(a);
      chip.setText(text);
      chip.setChipIconResource(R.drawable.ic_auto_awesome);
      chip.setChipIconVisible(true);
      chip.setChipIconTint(ColorStateList.valueOf(Ui.primary(a)));
      chip.setOnClickListener(v -> input.setText(text));
      examples.addView(chip);
    }
    box.addView(examples, Ui.wrap(a, 0, 4));

    TextView status =
        Ui.note(
            a,
            "接口："
                + a.settings.get("endpoint", "https://api.deepseek.com")
                + "（可直接执行修改并展示操作明细）");
    box.addView(status);

    LoadingIndicator loading = new LoadingIndicator(a);
    loading.setVisibility(View.GONE);
    LinearLayout.LayoutParams loadingLp = new LinearLayout.LayoutParams(-2, -2);
    loadingLp.gravity = Gravity.CENTER_HORIZONTAL;
    box.addView(loading, loadingLp);

    LinearLayout answer = Ui.column(a);
    box.addView(answer);

    LinearLayout actionRow = Ui.row(a);
    MaterialButton send =
        Ui.withIcon(Ui.heroButton(a, "发送并执行", () -> {}), R.drawable.ic_send);
    LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(0, -2, 1);
    actionRow.addView(send, sendLp);
    box.addView(actionRow, Ui.wrap(a, 8, 4));

    box.addView(
        Ui.withIcon(
            Ui.textButton(a, "配置 DeepSeek API Key 与模型", () -> new SettingsScreen(a).api()),
            R.drawable.ic_key),
        new LinearLayout.LayoutParams(-1, -2));

    send.setOnClickListener(
        v -> {
          String question = Ui.value(input);
          if (question.isEmpty()) {
            input.setError("请输入你的安排需求");
            return;
          }
          if (question.length() > 6000) {
            input.setError("请求请保持在6000字以内");
            return;
          }
          final String key;
          try {
            key = new SecretStore(a).read();
            if (key.isEmpty()) {
              a.toast("请先配置 DeepSeek API Key");
              new SettingsScreen(a).api();
              return;
            }
          } catch (Exception e) {
            a.error(e);
            return;
          }
          Store.Snapshot base = a.store.snapshot();
          String term = a.settings.term().toString();
          send.setEnabled(false);
          loading.setVisibility(View.VISIBLE);
          status.setText("正在请求 DeepSeek 并处理课表 JSON…");
          answer.removeAllViews();
          a.worker.execute(
              () -> {
                try {
                  Plan plan =
                      new AssistantClient()
                          .propose(question, base, a.settings, key, new ArrayList<>(history));
                  List<Item> next = plan.preview(base.items(), LocalDate.parse(term));
                  List<String> summaryLines = plan.summarize(base.items(), next);
                  List<String> conflicts = Schedule.conflicts(next, LocalDate.parse(term));
                  boolean changed = !summaryLines.isEmpty();
                  if (changed) {
                    a.store.replace(base.revision(), next);
                    writeLocalScheduleJson(next);
                  }
                  a.runOnUiThread(
                      () -> {
                        if (a.isDestroyed()) return;
                        if (changed) a.refresh();
                        if (!dialog.isShowing()) return;
                        send.setEnabled(true);
                        loading.setVisibility(View.GONE);
                        status.setText(
                            changed
                                ? "已直接写入课表 JSON · 共完成 " + summaryLines.size() + " 项操作"
                                : "回复完成 · 未修改课表数据");
                        history.add(new AssistantClient.Turn("user", question));
                        history.add(new AssistantClient.Turn("assistant", Store.JSON.toJson(plan)));

                        MaterialCardView resultCard = Ui.surfaceCard(a);
                        resultCard.setLayoutParams(Ui.wrap(a, 8, 4));
                        LinearLayout resultBox = Ui.column(a);
                        Ui.inset(resultBox, 16);

                        TextView explanation = Ui.title(a, plan.message, Ui.ink(a));
                        explanation.setTextSize(15);
                        explanation.setTextIsSelectable(true);
                        resultBox.addView(explanation);

                        if (!summaryLines.isEmpty()) {
                          TextView opHeader =
                              Ui.text(
                                  a,
                                  "具体执行操作（" + summaryLines.size() + " 项）：",
                                  13,
                                  Ui.primary(a));
                          Ui.emphasize(opHeader, 700);
                          opHeader.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 4));
                          resultBox.addView(opHeader);

                          for (String line : summaryLines) {
                            TextView opRow = Ui.text(a, line, 13.5f, Ui.ink(a));
                            opRow.setPadding(0, Ui.dp(a, 2), 0, Ui.dp(a, 2));
                            resultBox.addView(opRow);
                          }

                          if (!conflicts.isEmpty()) {
                            TextView conflictView =
                                Ui.text(
                                    a,
                                    "时间冲突提示：\n" + String.join("\n", conflicts),
                                    12.5f,
                                    0xFFD93025);
                            conflictView.setPadding(0, Ui.dp(a, 6), 0, 0);
                            resultBox.addView(conflictView);
                          }

                          MaterialButton undoBtn =
                              Ui.tonalButton(
                                  a,
                                  "撤销本次修改",
                                  () -> {
                                    try {
                                      Store.Snapshot current = a.store.snapshot();
                                      a.store.replace(current.revision(), base.items());
                                      writeLocalScheduleJson(base.items());
                                      a.refresh();
                                      status.setText("已撤销刚才的 AI 修改");
                                      a.toast("已撤销本次修改");
                                    } catch (Exception ex) {
                                      a.error(ex);
                                    }
                                  });
                          resultBox.addView(undoBtn, Ui.wrap(a, 10, 0));
                        }
                        resultCard.addView(resultBox);
                        answer.addView(resultCard);
                      });
                } catch (Exception e) {
                  a.runOnUiThread(
                      () -> {
                        if (a.isDestroyed() || !dialog.isShowing()) return;
                        send.setEnabled(true);
                        loading.setVisibility(View.GONE);
                        status.setText("执行失败，请检查网络、API Key 或模型配置。");
                        a.error(e);
                      });
                }
              });
        });

    ScrollView scroll = Ui.scroll(a, box);
    scroll.setFillViewport(true);
    int screenH = a.getResources().getDisplayMetrics().heightPixels;
    scroll.setMinimumHeight((int) (screenH * 0.88f));
    dialog.setContentView(scroll);
    dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    dialog.getBehavior().setSkipCollapsed(true);
    dialog.getBehavior().setMaxHeight((int) (screenH * 0.96f));
    dialog.show();
  }

  private void writeLocalScheduleJson(List<Item> items) {
    try {
      String json = Backup.export(new Store.Snapshot(0, items), a.settings);
      File file = new File(a.getFilesDir(), "yiclass-schedule.json");
      try (FileOutputStream out = new FileOutputStream(file)) {
        out.write(json.getBytes(StandardCharsets.UTF_8));
      }
    } catch (Exception ignored) {
    }
  }

  private void showJsonEditor(BottomSheetDialog parentDialog) {
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 16);
    Store.Snapshot base = a.store.snapshot();
    String currentJson = Backup.export(base, a.settings);
    TextInputEditText jsonField =
        Ui.field(box, "课表 JSON（可直接编辑或粘贴完整 JSON 写入）", currentJson, true);
    jsonField.setMinLines(10);
    jsonField.setMaxLines(18);
    jsonField.setTextSize(12);

    new MaterialAlertDialogBuilder(a)
        .setTitle("直接读写课表 JSON")
        .setView(Ui.scroll(a, box))
        .setNegativeButton("关闭", null)
        .setNeutralButton(
            "导出文件",
            (d, w) -> a.backupWriter.launch("YIClass-" + LocalDate.now() + ".json"))
        .setPositiveButton(
            "直接写入生效",
            (d, w) -> {
              try {
                String raw = Ui.value(jsonField);
                Backup.Decoded decoded = Backup.decode(raw);
                a.store.replace(base.revision(), decoded.items());
                a.settings.put("term", decoded.term());
                a.settings.put("zone", decoded.zone());
                if (!decoded.starts().isEmpty()) a.settings.put("starts", decoded.starts());
                if (!decoded.ends().isEmpty()) a.settings.put("ends", decoded.ends());
                writeLocalScheduleJson(decoded.items());
                a.refresh();
                if (parentDialog != null && parentDialog.isShowing()) parentDialog.dismiss();
                a.toast("已直接写入 " + decoded.items().size() + " 项安排");
              } catch (Exception e) {
                a.error(e);
              }
            })
        .show();
  }
}
