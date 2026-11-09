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
import com.google.android.material.loadingindicator.LoadingIndicator;
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
    box.setPadding(Ui.dp(a, 24), 0, Ui.dp(a, 24), Ui.dp(a, 24));
    box.addView(Ui.sheetHandle(a));

    LinearLayout hero = Ui.column(a);
    hero.setGravity(Gravity.CENTER_HORIZONTAL);
    hero.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 14));
    ImageView mark = Ui.icon(a, R.drawable.ic_book, 56, Ui.ink(a));
    hero.addView(mark);
    TextView headTitle = Ui.heading(a, "我们应该安排什么？");
    headTitle.setGravity(Gravity.CENTER);
    headTitle.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 2));
    hero.addView(headTitle);
    box.addView(hero);

    TextInputEditText input = Ui.field(box, "说说你的安排…", "", true);
    Ui.startIcon(input, R.drawable.ic_edit_calendar);

    ChipGroup examples = new ChipGroup(a);
    examples.setChipSpacingVertical(Ui.dp(a, 4));
    for (String text :
        new String[] {"检查本周是否有时间冲突", "帮我在明天下午安排一小时自习", "把明天的篮球课调到周五下午"}) {
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
        Ui.note(a, "当前接口：" + a.settings.get("endpoint", "https://api.deepseek.com"));
    box.addView(status);
    LoadingIndicator loading = new LoadingIndicator(a);
    loading.setVisibility(View.GONE);
    LinearLayout.LayoutParams loadingLp = new LinearLayout.LayoutParams(-2, -2);
    loadingLp.gravity = Gravity.CENTER_HORIZONTAL;
    box.addView(loading, loadingLp);
    LinearLayout answer = Ui.column(a);
    box.addView(answer);
    MaterialButton send =
        Ui.withIcon(Ui.heroButton(a, "发送请求", () -> {}), R.drawable.ic_send);
    LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(-1, -2);
    sendLp.topMargin = Ui.dp(a, 8);
    sendLp.bottomMargin = Ui.dp(a, 4);
    box.addView(send, sendLp);
    box.addView(
        Ui.withIcon(
            Ui.textButton(a, "配置 AI 服务", () -> new SettingsScreen(a).api()), R.drawable.ic_key),
        new LinearLayout.LayoutParams(-1, -2));
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
          loading.setVisibility(View.VISIBLE);
          status.setText("正在整理…");
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
                        loading.setVisibility(View.GONE);
                        status.setText("方案已生成 · 确认后生效");
                        history.add(new AssistantClient.Turn("user", question));
                        history.add(new AssistantClient.Turn("assistant", Store.JSON.toJson(plan)));
                        MaterialCardView resultCard = Ui.surfaceCard(a);
                        resultCard.setLayoutParams(Ui.wrap(a, 8, 4));
                        LinearLayout resultBox = Ui.column(a);
                        Ui.inset(resultBox, 16);
                        TextView explanation = Ui.note(a, plan.message);
                        explanation.setTextIsSelectable(true);
                        resultBox.addView(explanation);
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
                          resultBox.addView(
                              Ui.note(
                                  a,
                                  detail
                                      + "\n"
                                      + (conflicts.isEmpty()
                                          ? "本地检查：未发现时间冲突。"
                                          : "本地检查发现冲突：\n" + String.join("\n", conflicts))));
                          var apply =
                              Ui.heroButton(
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
                          resultBox.addView(apply, Ui.wrap(a, 8, 0));
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
                        status.setText("未能生成方案，请检查网络、Key 和模型设置。");
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
