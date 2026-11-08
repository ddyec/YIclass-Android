package com.ddyec.yiclass.ui;

import android.view.*;
import android.widget.*;
import com.ddyec.yiclass.ai.AssistantClient;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.*;
import com.google.android.material.bottomsheet.*;
import com.google.android.material.textfield.TextInputEditText;
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
    Ui.inset(box, 22);
    box.addView(Ui.heading(a, "你的时间助手"));
    box.addView(Ui.note(a, "调课、安排自习、添加或删除日程。每次变更都先给你看，确认后保存。"));
    LinearLayout examples = Ui.column(a);
    TextInputEditText input = Ui.field(box, "想怎么安排？", "", true);
    for (String text : new String[] {"检查本周是否有时间冲突", "帮我在明天下午安排一小时自习", "把明天的篮球课调到周五下午"})
      examples.addView(Ui.button(a, text, () -> input.setText(text)));
    box.addView(examples);
    TextView status =
        Ui.note(
            a,
            "当前接口："
                + a.settings.get("endpoint", "https://api.deepseek.com")
                + "\n发送时会提交当前课表和这条请求，不发送 PDF 文件。请勿输入无关隐私。");
    box.addView(status);
    LinearLayout answer = Ui.column(a);
    box.addView(answer);
    com.google.android.material.button.MaterialButton send = Ui.button(a, "生成安排建议", () -> {});
    box.addView(send);
    box.addView(Ui.button(a, "配置 AI 服务", () -> new SettingsScreen(a).api()));
    send.setOnClickListener(
        v -> {
          String question = Ui.value(input);
          if (question.isEmpty()) {
            input.setError("请输入你的安排需求");
            return;
          }
          if (question.length() > 4000) {
            input.setError("请求请保持在4000字以内");
            return;
          }
          final String key;
          try {
            key = new SecretStore(a).read();
            if (key.isEmpty()) {
              a.toast("先配置 API Key，再发送请求");
              new SettingsScreen(a).api();
              return;
            }
          } catch (Exception e) {
            a.error(e);
            return;
          }
          Store.Snapshot base = a.store.snapshot();
          String term = a.settings.term().toString();
          String zone = a.settings.zone().getId();
          send.setEnabled(false);
          status.setText("正在生成建议…");
          answer.removeAllViews();
          a.worker.execute(
              () -> {
                try {
                  Plan plan =
                      new AssistantClient()
                          .propose(question, base, a.settings, key, new ArrayList<>(history));
                  List<Item> next = plan.preview(base.items(), LocalDate.parse(term));
                  List<String> conflicts = Schedule.conflicts(next, LocalDate.parse(term));
                  a.runOnUiThread(
                      () -> {
                        if (a.isDestroyed() || !dialog.isShowing()) return;
                        send.setEnabled(true);
                        status.setText("建议已生成 · 尚未修改课表");
                        history.add(new AssistantClient.Turn("user", question));
                        history.add(new AssistantClient.Turn("assistant", Store.JSON.toJson(plan)));
                        TextView explanation = Ui.note(a, plan.message);
                        explanation.setTextIsSelectable(true);
                        answer.addView(explanation);
                        if (!plan.operations.isEmpty()) {
                          StringBuilder detail = new StringBuilder();
                          for (Plan.Operation op : plan.operations) {
                            Item old =
                                base.items().stream()
                                    .filter(i -> i.id.equals(op.id))
                                    .findFirst()
                                    .orElse(null);
                            detail
                                .append(
                                    switch (op.action) {
                                      case "add" -> "添加";
                                      case "delete" -> "删除";
                                      default -> "修改";
                                    })
                                .append(" · ")
                                .append(
                                    op.item != null
                                        ? op.item.title
                                        : old == null ? op.id : old.title)
                                .append(
                                    op.scopeDate.isEmpty() ? "（整个安排）" : "（仅 " + op.scopeDate + "）")
                                .append('\n');
                            if (op.item != null)
                              detail
                                  .append(
                                      op.item.date.isEmpty()
                                          ? Ui.day(op.item.day) + " " + op.item.weeks
                                          : op.item.date)
                                  .append("  ")
                                  .append(Schedule.time(op.item.start))
                                  .append("–")
                                  .append(Schedule.time(op.item.end))
                                  .append("\n地点：")
                                  .append(op.item.location)
                                  .append('\n');
                          }
                          answer.addView(
                              Ui.note(
                                  a,
                                  detail
                                      + "\n"
                                      + (conflicts.isEmpty()
                                          ? "本地检查：未发现时间冲突。"
                                          : "本地检查发现冲突：\n" + String.join("\n", conflicts))));
                          var apply =
                              Ui.button(
                                  a,
                                  "审阅并应用 " + plan.operations.size() + " 项变更",
                                  () -> {
                                    if (!a.settings.term().toString().equals(term)
                                        || !a.settings.zone().getId().equals(zone)) {
                                      a.error(new IllegalStateException("学期或时区设置已变化，请重新生成建议"));
                                      return;
                                    }
                                    a.review(
                                        "应用 AI 建议",
                                        base,
                                        next,
                                        detail.toString(),
                                        () -> {
                                          a.save(base.revision(), next);
                                          dialog.dismiss();
                                        });
                                  });
                          answer.addView(apply);
                        }
                      });
                } catch (Exception e) {
                  a.runOnUiThread(
                      () -> {
                        if (a.isDestroyed() || !dialog.isShowing()) return;
                        send.setEnabled(true);
                        status.setText("未能生成建议，课表没有变化。请检查网络、Key和模型设置。");
                        a.error(e);
                      });
                }
              });
        });
    dialog.setContentView(Ui.scroll(a, box));
    dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    dialog
        .getBehavior()
        .setMaxHeight((int) (a.getResources().getDisplayMetrics().heightPixels * .92));
    dialog.show();
  }
}
