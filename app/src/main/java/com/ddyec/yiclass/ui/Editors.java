package com.ddyec.yiclass.ui;

import android.view.*;
import android.widget.*;
import com.ddyec.yiclass.R;
import com.ddyec.yiclass.core.*;
import com.ddyec.yiclass.data.Store;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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
    box.setPadding(Ui.dp(a, 24), 0, Ui.dp(a, 24), Ui.dp(a, 24));
    box.addView(Ui.sheetHandle(a));

    TextView headTitle =
        Ui.heading(
            a,
            original == null ? (event ? "新建日程" : "添加课程") : occurrence == null ? "编辑安排" : "编辑本次课程");
    box.addView(headTitle, Ui.wrap(a, 0, 8));
    if (occurrence != null && original != null)
      box.addView(Ui.note(a, "仅修改 " + occurrence + "，其他周次不受影响。"));

    int[] typeIndex = {"EVENT".equals(draft.type) ? 1 : 0};
    View[] courseOnly = new View[2];
    LinearLayout type =
        Ui.segmentedControl(
            a,
            new String[] {"课程", "日程"},
            typeIndex[0],
            n -> {
              typeIndex[0] = n;
              for (View v : courseOnly)
                if (v != null) v.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
            });
    LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(-1, Ui.dp(a, 38));
    typeLp.bottomMargin = Ui.dp(a, 14);
    box.addView(type, typeLp);

    TextInputEditText title = Ui.field(box, "名称", draft.title, false);
    TextInputEditText location = Ui.field(box, "地点", draft.location, false);
    Ui.startIcon(location, R.drawable.ic_location);
    TextInputEditText teacher = Ui.field(box, "教师 / 联系人", draft.teacher, false);
    Ui.startIcon(teacher, R.drawable.ic_person);
    TextInputEditText date = Ui.field(box, "指定日期 YYYY-MM-DD（循环课留空）", draft.date, false);
    date.setInputType(
        android.text.InputType.TYPE_CLASS_DATETIME
            | android.text.InputType.TYPE_DATETIME_VARIATION_DATE);
    Ui.endAction(date, R.drawable.ic_event, "选择日期", () -> pickDate(date));

    LinearLayout dayBlock = Ui.column(a);
    dayBlock.addView(Ui.label(a, "上课星期", Ui.muted(a)), Ui.wrap(a, 0, 6));
    int[] dayIndex = {Math.max(0, Math.min(6, draft.day - 1))};
    MaterialButtonToggleGroup day =
        Ui.connectedGroup(
            a,
            new String[] {"一", "二", "三", "四", "五", "六", "日"},
            null,
            dayIndex[0],
            0,
            n -> dayIndex[0] = n);
    dayBlock.addView(day, new LinearLayout.LayoutParams(-1, Ui.dp(a, 44)));
    box.addView(dayBlock, Ui.wrap(a, 0, 12));
    TextInputEditText weeks = Ui.field(box, "循环周次，例如 1-16 或 1-15(单)", draft.weeks, false);
    courseOnly[0] = dayBlock;
    courseOnly[1] = Ui.layoutOf(weeks);
    if (typeIndex[0] == 1)
      for (View v : courseOnly) if (v != null) v.setVisibility(View.GONE);

    LinearLayout times = Ui.row(a);
    times.setGravity(Gravity.TOP);
    LinearLayout startCol = Ui.column(a), endCol = Ui.column(a);
    TextInputEditText start = Ui.field(startCol, "开始 HH:mm", Schedule.time(draft.start), false);
    TextInputEditText end = Ui.field(endCol, "结束 HH:mm", Schedule.time(draft.end), false);
    Ui.endAction(start, R.drawable.ic_schedule, "选择开始时间", () -> pickTime(start, "开始时间"));
    Ui.endAction(end, R.drawable.ic_schedule, "选择结束时间", () -> pickTime(end, "结束时间"));
    LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
    half.rightMargin = Ui.dp(a, 6);
    times.addView(startCol, half);
    LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, -2, 1);
    half2.leftMargin = Ui.dp(a, 6);
    times.addView(endCol, half2);
    box.addView(times);
    TextInputEditText reminder =
        Ui.field(box, "提前提醒（分钟，-1为关闭）", String.valueOf(draft.reminder), false);
    reminder.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
    Ui.startIcon(reminder, R.drawable.ic_notifications);

    box.addView(Ui.label(a, "颜色", Ui.muted(a)), Ui.wrap(a, 0, 8));
    int[] colorIndex = {Math.floorMod(draft.color, 6)};
    String[] colorNames = {"玫瑰", "鸢尾", "薄荷", "杏黄", "浅紫", "雾蓝"};
    LinearLayout swatches = Ui.row(a);
    List<FrameLayout> swatchViews = new ArrayList<>();
    Runnable paintSwatches =
        () -> {
          for (int i = 0; i < swatchViews.size(); i++) {
            boolean on = i == colorIndex[0];
            FrameLayout f = swatchViews.get(i);
            f.getChildAt(0).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            f.setSelected(on);
          }
        };
    for (int i = 0; i < 6; i++) {
      final int index = i;
      FrameLayout f =
          Ui.shapeIcon(
              a,
              ExpressiveShapes.CIRCLE,
              R.drawable.ic_check,
              Ui.courseAccent(a, i),
              Ui.courseColor(a, i),
              38,
              20);
      f.setContentDescription(colorNames[i]);
      f.setClickable(true);
      f.setOnClickListener(
          v -> {
            colorIndex[0] = index;
            paintSwatches.run();
          });
      swatchViews.add(f);
      LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(a, 38), Ui.dp(a, 38));
      sp.rightMargin = Ui.dp(a, 12);
      swatches.addView(f, sp);
    }
    paintSwatches.run();
    box.addView(swatches, Ui.wrap(a, 0, 14));
    TextInputEditText notes = Ui.field(box, "备注", draft.notes, true);
    MaterialButton saveBtn =
        Ui.heroButton(
            a,
            "保存",
            () -> {
              try {
                draft.title = Ui.value(title);
                draft.location = Ui.value(location);
                draft.teacher = Ui.value(teacher);
                draft.notes = Ui.value(notes);
                draft.type = typeIndex[0] == 0 ? "COURSE" : "EVENT";
                draft.date = Ui.value(date);
                draft.day = dayIndex[0] + 1;
                draft.weeks = Ui.value(weeks);
                draft.start = Schedule.minute(Ui.value(start));
                draft.end = Schedule.minute(Ui.value(end));
                draft.reminder = Integer.parseInt(Ui.value(reminder));
                draft.color = colorIndex[0];
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
            });
    Ui.withIcon(saveBtn, R.drawable.ic_check);
    LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, -2);
    saveLp.topMargin = Ui.dp(a, 8);
    saveLp.bottomMargin = Ui.dp(a, 4);
    box.addView(saveBtn, saveLp);
    box.addView(Ui.textButton(a, "取消", dialog::dismiss), new LinearLayout.LayoutParams(-1, -2));
    dialog.setContentView(Ui.scroll(a, box));
    dialog
        .getBehavior()
        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
    dialog
        .getBehavior()
        .setMaxHeight((int) (a.getResources().getDisplayMetrics().heightPixels * .92));
    dialog.show();
  }

  private void pickTime(TextInputEditText field, String title) {
    int minute;
    try {
      minute = Schedule.minute(Ui.value(field));
    } catch (Exception ignored) {
      minute = 510;
    }
    MaterialTimePicker picker =
        new MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(minute / 60)
            .setMinute(minute % 60)
            .setTitleText(title)
            .build();
    picker.addOnPositiveButtonClickListener(
        v -> field.setText(Schedule.time(picker.getHour() * 60 + picker.getMinute())));
    picker.show(a.getSupportFragmentManager(), "time");
  }

  private void pickDate(TextInputEditText field) {
    LocalDate initial;
    try {
      initial = LocalDate.parse(Ui.value(field));
    } catch (Exception ignored) {
      initial = a.selected;
    }
    MaterialDatePicker<Long> picker =
        MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            .build();
    picker.addOnPositiveButtonClickListener(
        millis ->
            field.setText(
                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()));
    picker.show(a.getSupportFragmentManager(), "date");
  }
}
