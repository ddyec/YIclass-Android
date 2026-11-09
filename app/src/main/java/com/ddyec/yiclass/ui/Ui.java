package com.ddyec.yiclass.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import androidx.core.widget.TextViewCompat;
import androidx.graphics.shapes.RoundedPolygon;
import com.ddyec.yiclass.R;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.textfield.*;
import java.util.function.IntConsumer;

final class Ui {
  private Ui() {}

  static int dp(Context c, float n) {
    return (int) (n * c.getResources().getDisplayMetrics().density + .5f);
  }

  static int color(Context c, int attr) {
    TypedValue v = new TypedValue();
    c.getTheme().resolveAttribute(attr, v, true);
    return v.data;
  }

  static int styleRes(Context c, int attr) {
    TypedValue v = new TypedValue();
    c.getTheme().resolveAttribute(attr, v, true);
    return v.resourceId;
  }

  static boolean isDark(Context c) {
    return (c.getResources().getConfiguration().uiMode & 48) == 32;
  }

  static int ink(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnSurface);
  }

  static int muted(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnSurfaceVariant);
  }

  static int primary(Context c) {
    return color(c, androidx.appcompat.R.attr.colorPrimary);
  }

  static int onPrimary(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnPrimary);
  }

  static int primaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorPrimaryContainer);
  }

  static int onPrimaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnPrimaryContainer);
  }

  static int secondaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorSecondaryContainer);
  }

  static int onSecondaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnSecondaryContainer);
  }

  static int tertiary(Context c) {
    return color(c, com.google.android.material.R.attr.colorTertiary);
  }

  static int tertiaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorTertiaryContainer);
  }

  static int onTertiaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnTertiaryContainer);
  }

  static int surface(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurface);
  }

  static int surfaceLowest(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurfaceContainerLowest);
  }

  static int surfaceLow(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurfaceContainerLow);
  }

  static int surfaceContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurfaceContainer);
  }

  static int surfaceHigh(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurfaceContainerHigh);
  }

  static int surfaceHighest(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurfaceContainerHighest);
  }

  static int outlineVariant(Context c) {
    return color(c, com.google.android.material.R.attr.colorOutlineVariant);
  }

  static LinearLayout column(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  static LinearLayout row(Context c) {
    LinearLayout l = new LinearLayout(c);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  static ScrollView scroll(Context c, View child) {
    ScrollView v = new ScrollView(c);
    v.setFillViewport(true);
    v.setClipToPadding(false);
    v.addView(child);
    return v;
  }

  static void inset(LinearLayout l, int amount) {
    int p = dp(l.getContext(), amount);
    l.setPadding(p, p, p, p);
  }

  static LinearLayout.LayoutParams wrap(Context c, int topDp, int bottomDp) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(c, topDp);
    lp.bottomMargin = dp(c, bottomDp);
    return lp;
  }

  static TextView text(Context c, String s, float sp, int color) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color);
    return t;
  }

  static TextView styled(Context c, String s, int textAppearanceAttr, int color) {
    TextView t = new TextView(c);
    int res = styleRes(c, textAppearanceAttr);
    if (res != 0) TextViewCompat.setTextAppearance(t, res);
    t.setText(s);
    t.setTextColor(color);
    return t;
  }

  static void emphasize(TextView t, int weight) {
    if (Build.VERSION.SDK_INT >= 28)
      t.setTypeface(
          Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), weight, false));
    else
      t.setTypeface(
          Typeface.create(
              "sans-serif-medium", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
  }

  static TextView display(Context c, String s) {
    TextView t =
        styled(c, s, com.google.android.material.R.attr.textAppearanceHeadlineLarge, ink(c));
    emphasize(t, 700);
    t.setLetterSpacing(-0.01f);
    return t;
  }

  static TextView heading(Context c, String s) {
    TextView t =
        styled(c, s, com.google.android.material.R.attr.textAppearanceHeadlineSmall, ink(c));
    emphasize(t, 700);
    t.setPadding(0, dp(c, 8), 0, dp(c, 6));
    return t;
  }

  static TextView title(Context c, String s, int color) {
    TextView t = styled(c, s, com.google.android.material.R.attr.textAppearanceTitleLarge, color);
    emphasize(t, 700);
    return t;
  }

  static TextView subheading(Context c, String s) {
    TextView t =
        styled(c, s, com.google.android.material.R.attr.textAppearanceTitleMedium, ink(c));
    emphasize(t, 600);
    return t;
  }

  static TextView label(Context c, String s, int color) {
    TextView t = styled(c, s, com.google.android.material.R.attr.textAppearanceLabelLarge, color);
    emphasize(t, 600);
    return t;
  }

  static TextView note(Context c, String s) {
    TextView t =
        styled(c, s, com.google.android.material.R.attr.textAppearanceBodyMedium, muted(c));
    t.setLineSpacing(dp(c, 3), 1);
    t.setPadding(0, dp(c, 6), 0, dp(c, 6));
    return t;
  }

  static TextView sectionHeader(Context c, String s) {
    TextView t = label(c, s, primary(c));
    t.setPadding(dp(c, 16), dp(c, 18), dp(c, 16), dp(c, 8));
    return t;
  }

  static GradientDrawable roundedBg(
      Context c, int fill, float radiusDp, int strokeWidthDp, int strokeColor) {
    GradientDrawable d = new GradientDrawable();
    d.setShape(GradientDrawable.RECTANGLE);
    d.setColor(fill);
    d.setCornerRadius(dp(c, radiusDp));
    if (strokeWidthDp > 0) d.setStroke(dp(c, strokeWidthDp), strokeColor);
    return d;
  }

  static Drawable expressiveShape(RoundedPolygon polygon, int color) {
    ShapeDrawable d = ExpressiveShapes.drawable(polygon);
    d.getPaint().setColor(color);
    d.getPaint().setAntiAlias(true);
    return d;
  }

  static ImageView icon(Context c, int res, int sizeDp, int tint) {
    ImageView v = new ImageView(c);
    v.setImageResource(res);
    v.setImageTintList(ColorStateList.valueOf(tint));
    v.setScaleType(ImageView.ScaleType.FIT_CENTER);
    v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
    return v;
  }

  static FrameLayout circleButton(
      Context c, int iconRes, String description, int iconTint, Runnable action) {
    FrameLayout btn = new FrameLayout(c);
    int fill = isDark(c) ? 0xFF1F222B : 0xFFE9ECF4;
    int stroke = isDark(c) ? 0xFF2E323E : 0xFFD9DEE9;
    btn.setBackground(roundedBg(c, fill, 999, 1, stroke));
    ImageView img = new ImageView(c);
    img.setImageResource(iconRes);
    img.setImageTintList(ColorStateList.valueOf(iconTint));
    img.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    btn.addView(img, new FrameLayout.LayoutParams(dp(c, 22), dp(c, 22), Gravity.CENTER));
    btn.setContentDescription(description);
    androidx.appcompat.widget.TooltipCompat.setTooltipText(btn, description);
    btn.setClickable(true);
    btn.setFocusable(true);
    btn.setOnClickListener(v -> action.run());
    btn.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 42), dp(c, 42)));
    return btn;
  }

  static LinearLayout segmentedControl(
      Context c, String[] labels, int checked, IntConsumer onPick) {
    LinearLayout track = row(c);
    int trackFill = isDark(c) ? 0xFF1C1D24 : 0xFFE6E9F2;
    track.setBackground(roundedBg(c, trackFill, 999, 0, 0));
    int pad = dp(c, 3);
    track.setPadding(pad, pad, pad, pad);
    int thumbFill = isDark(c) ? 0xFF606168 : 0xFFFFFFFF;
    for (int i = 0; i < labels.length; i++) {
      final int idx = i;
      boolean active = i == checked;
      TextView tab = text(c, labels[i], 14, active ? ink(c) : (isDark(c) ? 0xFFD5D7E0 : muted(c)));
      emphasize(tab, active ? 700 : 500);
      tab.setGravity(Gravity.CENTER);
      if (active) tab.setBackground(roundedBg(c, thumbFill, 999, 0, 0));
      tab.setContentDescription(labels[i]);
      tab.setClickable(true);
      tab.setFocusable(true);
      tab.setOnClickListener(v -> onPick.accept(idx));
      track.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
    }
    return track;
  }

  static FrameLayout shapeIcon(
      Context c, RoundedPolygon polygon, int iconRes, int bg, int fg, int sizeDp, int iconDp) {
    FrameLayout f = new FrameLayout(c);
    f.setBackground(expressiveShape(polygon, bg));
    ImageView i = new ImageView(c);
    i.setImageResource(iconRes);
    i.setImageTintList(ColorStateList.valueOf(fg));
    i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    f.addView(i, new FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER));
    f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
    return f;
  }

  static ShapeAppearanceModel corners(Context c, float top, float bottom) {
    return ShapeAppearanceModel.builder()
        .setTopLeftCornerSize(dp(c, top))
        .setTopRightCornerSize(dp(c, top))
        .setBottomLeftCornerSize(dp(c, bottom))
        .setBottomRightCornerSize(dp(c, bottom))
        .build();
  }

  static ShapeAppearanceModel segment(Context c, int position, int count) {
    float outer = 20, inner = 6;
    boolean first = position == 0, last = position == count - 1;
    return corners(c, first ? outer : inner, last ? outer : inner);
  }

  static TextView pillBadge(Context c, String label, int bg, int fg) {
    TextView t = styled(c, label, com.google.android.material.R.attr.textAppearanceLabelMedium, fg);
    emphasize(t, 600);
    t.setSingleLine(true);
    t.setEllipsize(TextUtils.TruncateAt.END);
    t.setGravity(Gravity.CENTER);
    t.setBackground(roundedBg(c, bg, 999, 0, 0));
    t.setPadding(dp(c, 12), dp(c, 5), dp(c, 12), dp(c, 5));
    return t;
  }

  static int successContainer(Context c) {
    return isDark(c) ? 0xff1f4d3f : 0xffc8f0dc;
  }

  static int onSuccessContainer(Context c) {
    return isDark(c) ? 0xffa6f2d2 : 0xff0b4f3b;
  }

  static TextView statusBadge(Context c, String label, boolean active) {
    int bg = active ? successContainer(c) : tertiaryContainer(c);
    int fg = active ? onSuccessContainer(c) : onTertiaryContainer(c);
    return pillBadge(c, label, bg, fg);
  }

  static View sheetHandle(Context c) {
    BottomSheetDragHandleView handle = new BottomSheetDragHandleView(c);
    handle.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
    return handle;
  }

  private static MaterialButton make(Context c, int styleAttr, String text, Runnable action) {
    MaterialButton b = new MaterialButton(c, null, styleAttr);
    b.setText(text);
    b.setAllCaps(false);
    b.setMaxLines(text.contains("\n") ? 2 : 1);
    b.setEllipsize(TextUtils.TruncateAt.END);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  static MaterialButton button(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonOutlinedStyle, text, action);
  }

  static MaterialButton textButton(Context c, String text, Runnable action) {
    return make(c, androidx.appcompat.R.attr.borderlessButtonStyle, text, action);
  }

  static MaterialButton tonalButton(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonTonalStyle, text, action);
  }

  static MaterialButton filledButton(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonStyle, text, action);
  }

  static MaterialButton heroButton(Context c, String text, Runnable action) {
    return make(c, R.attr.yiButtonFilledMedium, text, action);
  }

  static MaterialButton withIcon(MaterialButton b, int iconRes) {
    b.setIconResource(iconRes);
    b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
    return b;
  }

  static MaterialButton iconButton(
      Context c, int iconRes, String description, boolean tonal, Runnable action) {
    MaterialButton b =
        new MaterialButton(
            c,
            null,
            tonal
                ? com.google.android.material.R.attr.materialIconButtonFilledTonalStyle
                : com.google.android.material.R.attr.materialIconButtonStyle);
    b.setIconResource(iconRes);
    b.setContentDescription(description);
    androidx.appcompat.widget.TooltipCompat.setTooltipText(b, description);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  static MaterialButtonToggleGroup connectedGroup(
      Context c, String[] labels, int[] icons, int checked, float textSp, IntConsumer onPick) {
    MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(c);
    group.setSingleSelection(true);
    group.setSelectionRequired(true);
    int[] ids = new int[labels.length];
    for (int i = 0; i < labels.length; i++) {
      MaterialButton b =
          new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonStyle);
      ids[i] = View.generateViewId();
      b.setId(ids[i]);
      b.setText(labels[i]);
      b.setAllCaps(false);
      if (textSp > 0) b.setTextSize(textSp);
      if (labels[i].contains("\n")) {
        b.setSingleLine(false);
        b.setMaxLines(2);
        b.setEllipsize(null);
        b.setLineSpacing(0, 0.95f);
      } else {
        b.setMaxLines(1);
        b.setEllipsize(TextUtils.TruncateAt.END);
      }
      if (icons != null && icons[i] != 0) {
        b.setIconResource(icons[i]);
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
      }
      b.setPadding(dp(c, 4), 0, dp(c, 4), 0);
      group.addView(b, new LinearLayout.LayoutParams(0, -1, 1));
    }
    if (checked >= 0 && checked < ids.length) group.check(ids[checked]);
    group.addOnButtonCheckedListener(
        (g, id, isChecked) -> {
          if (!isChecked) return;
          for (int i = 0; i < ids.length; i++) if (ids[i] == id) onPick.accept(i);
        });
    return group;
  }

  static MaterialCardView card(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 16));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(surfaceLow(c));
    return v;
  }

  static MaterialCardView surfaceCard(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 22));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(surfaceContainer(c));
    return v;
  }

  static MaterialCardView listItem(
      Context c,
      int iconRes,
      String title,
      String supporting,
      View trailing,
      int position,
      int count,
      Runnable action) {
    MaterialCardView card = new MaterialCardView(c);
    card.setCardElevation(0);
    card.setStrokeWidth(0);
    card.setCardBackgroundColor(surfaceLowest(c));
    card.setShapeAppearanceModel(segment(c, position, count));
    LinearLayout row = row(c);
    row.setMinimumHeight(dp(c, 64));
    row.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
    if (iconRes != 0) {
      FrameLayout lead =
          shapeIcon(
              c,
              ExpressiveShapes.CIRCLE,
              iconRes,
              secondaryContainer(c),
              onSecondaryContainer(c),
              38,
              20);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 38), dp(c, 38));
      lp.rightMargin = dp(c, 14);
      row.addView(lead, lp);
    }
    LinearLayout texts = column(c);
    TextView t = styled(c, title, com.google.android.material.R.attr.textAppearanceBodyLarge, ink(c));
    emphasize(t, 500);
    texts.addView(t);
    if (supporting != null && !supporting.isEmpty()) {
      TextView s =
          styled(c, supporting, com.google.android.material.R.attr.textAppearanceBodyMedium, muted(c));
      s.setPadding(0, dp(c, 2), 0, 0);
      texts.addView(s);
    }
    row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
    if (trailing != null) {
      LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
      tp.leftMargin = dp(c, 12);
      row.addView(trailing, tp);
    }
    card.addView(row);
    if (action != null) card.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(c, position == count - 1 ? 0 : 2);
    card.setLayoutParams(lp);
    return card;
  }

  static TextView trailingValue(Context c, String value) {
    TextView t =
        styled(c, value, com.google.android.material.R.attr.textAppearanceLabelLarge, primary(c));
    t.setMaxWidth(dp(c, 140));
    t.setSingleLine(true);
    t.setEllipsize(TextUtils.TruncateAt.END);
    return t;
  }

  static View emptyState(
      Context c,
      int iconRes,
      String title,
      String note,
      String button,
      int buttonIcon,
      Runnable action) {
    MaterialCardView card = surfaceCard(c);
    card.setRadius(dp(c, 24));
    LinearLayout box = column(c);
    box.setPadding(dp(c, 24), dp(c, 28), dp(c, 24), dp(c, 24));
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    FrameLayout mark =
        shapeIcon(
            c,
            ExpressiveShapes.CIRCLE,
            iconRes,
            primaryContainer(c),
            onPrimaryContainer(c),
            64,
            30);
    box.addView(mark);
    TextView h = heading(c, title);
    h.setGravity(Gravity.CENTER);
    h.setPadding(0, dp(c, 14), 0, dp(c, 4));
    box.addView(h);
    TextView message = note(c, note);
    message.setGravity(Gravity.CENTER);
    box.addView(message);
    MaterialButton cta = withIcon(filledButton(c, button, action), buttonIcon);
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
    bp.topMargin = dp(c, 10);
    box.addView(cta, bp);
    card.addView(box);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(c, 8);
    card.setLayoutParams(lp);
    return card;
  }

  static TextInputEditText field(
      LinearLayout parent, String label, String value, boolean multiline) {
    Context c = parent.getContext();
    TextInputLayout wrapper = new TextInputLayout(c, null, R.attr.yiTextField);
    wrapper.setHint(label);
    TextInputEditText e = new TextInputEditText(wrapper.getContext());
    e.setText(value);
    e.setSingleLine(!multiline);
    if (multiline) {
      e.setMinLines(2);
      e.setMaxLines(5);
      e.setGravity(Gravity.TOP);
    }
    wrapper.addView(e, new LinearLayout.LayoutParams(-1, -2));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(c, 12);
    parent.addView(wrapper, lp);
    return e;
  }

  static TextInputLayout layoutOf(View e) {
    ViewParent p = e.getParent();
    while (p != null && !(p instanceof TextInputLayout)) p = p.getParent();
    return (TextInputLayout) p;
  }

  static void startIcon(TextInputEditText e, int iconRes) {
    TextInputLayout l = layoutOf(e);
    if (l != null) l.setStartIconDrawable(iconRes);
  }

  static void endAction(TextInputEditText e, int iconRes, String description, Runnable action) {
    TextInputLayout l = layoutOf(e);
    if (l == null) return;
    l.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
    l.setEndIconDrawable(iconRes);
    l.setEndIconContentDescription(description);
    l.setEndIconOnClickListener(v -> action.run());
  }

  static String value(EditText e) {
    return e.getText() == null ? "" : e.getText().toString().trim();
  }

  static String day(int day) {
    return new String[] {"周一", "周二", "周三", "周四", "周五", "周六", "周日"}[day - 1];
  }

  static int courseColor(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xff4a2835, 0xff233362, 0xff1b433b, 0xff4c381c, 0xff3f2854, 0xff1d3d56}
            : new int[] {0xffffe4ea, 0xffe2e8ff, 0xffd2f4ea, 0xffffebca, 0xfff3e2ff, 0xffd6ecff})
        [Math.floorMod(index, 6)];
  }

  static int courseAccent(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffff8da9, 0xff82a4ff, 0xff5fd4b8, 0xfff5b952, 0xffd494ff, 0xff6ec0f7}
            : new int[] {0xffc83e65, 0xff2855d6, 0xff167966, 0xff9c6200, 0xff7e3fa3, 0xff16699e})
        [Math.floorMod(index, 6)];
  }

  static int courseInk(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffffe3ea, 0xffe2e8ff, 0xffd4f5ec, 0xffffeccb, 0xfff4e3ff, 0xffd8edff}
            : new int[] {0xff4a1023, 0xff102978, 0xff063c32, 0xff382100, 0xff320b4c, 0xff05233b})
        [Math.floorMod(index, 6)];
  }
}
