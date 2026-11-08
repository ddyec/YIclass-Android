package com.ddyec.yiclass.ui;

import android.app.TimePickerDialog;
import android.view.*;
import android.widget.*;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.Store;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.time.*;
import java.util.*;

final class Editors {
  private final MainActivity a;

  Editors(MainActivity a) {
    this.a = a;
  }

  void edit(Item original, LocalDate occurrence, boolean event) {
    Store.Snapshot base = a.store.snapshot();
    Item draft =
        original == null
            ? new Item()
            : Store.JSON.fromJson(Store.JSON.toJson(original), Item.class);
    if (original == null) {
      draft.type = event ? "EVENT" : "COURSE";
      draft.day = a.selected.getDayOfWeek().getValue();
      if (event) draft.date = (occurrence == null ? a.selected : occurrence).toString();
    }
    if (original != null && occurrence != null) draft.date = occurrence.toString();
    BottomSheetDialog dialog = new BottomSheetDialog(a);
    LinearLayout box = Ui.column(a);
    Ui.inset(box, 22);
    box.addView(
        Ui.heading(
            a,
            original == null ? (event ? "新建日程" : "添加课程") : occurrence == null ? "编辑安排" : "编辑本次课程"));
    if (occurrence != null && original != null)
      box.addView(Ui.note(a, "仅修改 " + occurrence + "，其他周次不受影响。"));
    TextInputEditText title = Ui.field(box, "名称", draft.title, false);
    TextInputEditText location = Ui.field(box, "地点", draft.location, false);
    TextInputEditText teacher = Ui.field(box, "教师 / 联系人", draft.teacher, false);
    Spinner type = new Spinner(a);
    type.setAdapter(
        new ArrayAdapter<>(
            a,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"循环课程 / 单次课程", "日程与待办"}));
    type.setSelection("EVENT".equals(draft.type) ? 1 : 0);
    box.addView(type, new LinearLayout.LayoutParams(-1, Ui.dp(a, 48)));
    TextInputEditText date = Ui.field(box, "指定日期 YYYY-MM-DD（循环课留空）", draft.date, false);
    date.setInputType(
        android.text.InputType.TYPE_CLASS_DATETIME
            | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
    Spinner day = new Spinner(a);
    day.setAdapter(
        new ArrayAdapter<>(
            a,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"周一", "周二", "周三", "周四", "周五", "周六", "周日"}));
    day.setSelection(draft.day - 1);
    box.addView(day, new LinearLayout.LayoutParams(-1, Ui.dp(a, 48)));
    TextInputEditText weeks = Ui.field(box, "循环周次，例如 1-16 或 1-15(单)", draft.weeks, false);
    TextInputEditText start = Ui.field(box, "开始时间 HH:mm", Schedule.time(draft.start), false);
    TextInputEditText end = Ui.field(box, "结束时间 HH:mm", Schedule.time(draft.end), false);
    start.setOnClickListener(v -> pickTime(start));
    end.setOnClickListener(v -> pickTime(end));
    TextInputEditText reminder =
        Ui.field(box, "提前提醒（分钟，-1为关闭）", String.valueOf(draft.reminder), false);
    reminder.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    Spinner color = new Spinner(a);
    color.setAdapter(
        new ArrayAdapter<>(
            a,
            android.R.layout.simple_spinner_dropdown_item,
            new String[] {"玫瑰", "鸢尾", "薄荷", "杏黄", "浅紫", "雾蓝"}));
    color.setSelection(Math.floorMod(draft.color, 6));
    box.addView(color, new LinearLayout.LayoutParams(-1, Ui.dp(a, 48)));
    TextInputEditText notes = Ui.field(box, "备注", draft.notes, true);
    box.addView(
        Ui.button(
            a,
            "保存安排",
            () -> {
              try {
                draft.title = Ui.value(title);
                draft.location = Ui.value(location);
                draft.teacher = Ui.value(teacher);
                draft.notes = Ui.value(notes);
                draft.type = type.getSelectedItemPosition() == 0 ? "COURSE" : "EVENT";
                draft.date = Ui.value(date);
                draft.day = day.getSelectedItemPosition() + 1;
                draft.weeks = Ui.value(weeks);
                draft.start = Schedule.minute(Ui.value(start));
                draft.end = Schedule.minute(Ui.value(end));
                draft.reminder = Integer.parseInt(Ui.value(reminder));
                draft.color = color.getSelectedItemPosition();
                if ("EVENT".equals(draft.type) && draft.date.isEmpty())
                  throw new IllegalArgumentException("日程需要指定日期");
                if (!draft.date.isEmpty())
                  draft.day = LocalDate.parse(draft.date).getDayOfWeek().getValue();
                draft.validate();
                List<Item> next = new ArrayList<>(base.items());
                if (original != null && occurrence != null) {
                  for (Item item : next)
                    if (item.id.equals(original.id)) item.excludedDates.add(occurrence.toString());
                  draft.id = UUID.randomUUID().toString();
                  draft.excludedDates = new HashSet<>();
                } else if (original != null) next.removeIf(i -> i.id.equals(original.id));
                next.add(draft);
                a.review(
                    "保存前检查",
                    base,
                    next,
                    draft.title
                        + "\n"
                        + (draft.date.isEmpty()
                            ? Ui.day(draft.day) + " · " + draft.weeks
                            : draft.date)
                        + "\n"
                        + Schedule.time(draft.start)
                        + "—"
                        + Schedule.time(draft.end),
                    () -> {
                      a.save(base.revision(), next);
                      dialog.dismiss();
                    });
              } catch (Exception e) {
                a.error(e);
              }
            }));
    box.addView(Ui.button(a, "取消", dialog::dismiss));
    dialog.setContentView(Ui.scroll(a, box));
    dialog
        .getBehavior()
        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    dialog
        .getBehavior()
        .setMaxHeight((int) (a.getResources().getDisplayMetrics().heightPixels * .92));
    dialog.show();
  }

  private void pickTime(TextInputEditText field) {
    int minute;
    try {
      minute = Schedule.minute(Ui.value(field));
    } catch (Exception ignored) {
      minute = 510;
    }
    new TimePickerDialog(
            a,
            (view, h, m) -> field.setText(Schedule.time(h * 60 + m)),
            minute / 60,
            minute % 60,
            true)
        .show();
  }
}
