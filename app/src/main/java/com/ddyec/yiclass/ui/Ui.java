package com.ddyec.yiclass.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.*;

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
    return color(c, com.google.android.material.R.attr.colorPrimary);
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

  static int tertiaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorTertiaryContainer);
  }

  static int onTertiaryContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnTertiaryContainer);
  }

  static int surface(Context c) {
    return color(c, com.google.android.material.R.attr.colorSurface);
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

  static TextView text(Context c, String s, float sp, int color) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color);
    return t;
  }

  static TextView heading(Context c, String s) {
    TextView t = text(c, s, 26, ink(c));
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    t.setLetterSpacing(-0.01f);
    t.setPadding(0, dp(c, 10), 0, dp(c, 8));
    return t;
  }

  static TextView subheading(Context c, String s) {
    TextView t = text(c, s, 17, ink(c));
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    return t;
  }

  static TextView note(Context c, String s) {
    TextView t = text(c, s, 14, muted(c));
    t.setLineSpacing(dp(c, 4), 1);
    t.setPadding(0, dp(c, 6), 0, dp(c, 6));
    return t;
  }

  static GradientDrawable roundedBg(Context c, int fill, float radiusDp, int strokeWidthDp, int strokeColor) {
    GradientDrawable d = new GradientDrawable();
    d.setShape(GradientDrawable.RECTANGLE);
    d.setColor(fill);
    d.setCornerRadius(dp(c, radiusDp));
    if (strokeWidthDp > 0) d.setStroke(dp(c, strokeWidthDp), strokeColor);
    return d;
  }

  static TextView pillBadge(Context c, String label, int bg, int fg) {
    TextView t = text(c, label, 11, fg);
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    t.setSingleLine(true);
    t.setGravity(Gravity.CENTER);
    t.setBackground(roundedBg(c, bg, 999, 0, 0));
    t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
    return t;
  }

  static TextView statusBadge(Context c, String label, boolean active) {
    int bg = active
        ? (isDark(c) ? 0xff1c4a3e : 0xffd1f4e8)
        : tertiaryContainer(c);
    int fg = active
        ? (isDark(c) ? 0xff9ef2d8 : 0xff0c4f3d)
        : onTertiaryContainer(c);
    return pillBadge(c, label, bg, fg);
  }

  static View sheetHandle(Context c) {
    LinearLayout wrap = row(c);
    wrap.setGravity(Gravity.CENTER);
    wrap.setPadding(0, dp(c, 4), 0, dp(c, 10));
    View bar = new View(c);
    bar.setBackground(roundedBg(c, outlineVariant(c), 999, 0, 0));
    wrap.addView(bar, new LinearLayout.LayoutParams(dp(c, 36), dp(c, 4)));
    return wrap;
  }

  static MaterialButton button(Context c, String text, Runnable action) {
    MaterialButton b =
        new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    b.setText(text);
    b.setAllCaps(false);
    b.setCornerRadius(dp(c, 20));
    b.setStrokeWidth(dp(c, 1));
    b.setStrokeColor(ColorStateList.valueOf(outlineVariant(c)));
    b.setBackgroundTintList(ColorStateList.valueOf(surfaceLow(c)));
    b.setTextColor(ink(c));
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setMinHeight(dp(c, 48));
    b.setMinWidth(dp(c, 48));
    b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
    b.setMaxLines(text.contains("\n") ? 2 : 1);
    b.setEllipsize(TextUtils.TruncateAt.END);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  static MaterialButton tonalButton(Context c, String text, Runnable action) {
    MaterialButton b = button(c, text, action);
    b.setStrokeWidth(0);
    b.setCornerRadius(dp(c, 22));
    b.setBackgroundTintList(ColorStateList.valueOf(secondaryContainer(c)));
    b.setTextColor(onSecondaryContainer(c));
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    return b;
  }

  static MaterialButton filledButton(Context c, String text, Runnable action) {
    MaterialButton b = button(c, text, action);
    b.setStrokeWidth(0);
    b.setCornerRadius(dp(c, 24));
    b.setBackgroundTintList(ColorStateList.valueOf(primary(c)));
    b.setTextColor(onPrimary(c));
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    return b;
  }

  static MaterialCardView card(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 24));
    v.setCardElevation(0);
    v.setStrokeWidth(dp(c, 1));
    v.setStrokeColor(outlineVariant(c));
    v.setCardBackgroundColor(surfaceLow(c));
    return v;
  }

  static MaterialCardView heroCard(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 28));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(primaryContainer(c));
    return v;
  }

  static MaterialCardView surfaceCard(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 24));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(surfaceContainer(c));
    return v;
  }

  static MaterialCardView permissionCard(
      Context c,
      String badgeSymbol,
      String title,
      String subtitle,
      String statusText,
      boolean granted,
      String actionText,
      Runnable action) {
    MaterialCardView card = new MaterialCardView(c);
    card.setRadius(dp(c, 26));
    card.setCardElevation(0);
    card.setStrokeWidth(dp(c, 1));
    card.setStrokeColor(granted ? outlineVariant(c) : primary(c));
    card.setCardBackgroundColor(granted ? surfaceLow(c) : surfaceContainer(c));

    LinearLayout body = column(c);
    inset(body, 18);

    LinearLayout topRow = row(c);
    TextView iconBox = text(c, badgeSymbol, 18, granted ? onSecondaryContainer(c) : onPrimaryContainer(c));
    iconBox.setGravity(Gravity.CENTER);
    iconBox.setTypeface(null, Typeface.BOLD);
    iconBox.setBackground(
        roundedBg(c, granted ? secondaryContainer(c) : primaryContainer(c), 16, 0, 0));
    topRow.addView(iconBox, new LinearLayout.LayoutParams(dp(c, 44), dp(c, 44)));

    LinearLayout titleCol = column(c);
    titleCol.setPadding(dp(c, 12), 0, dp(c, 8), 0);
    TextView titleView = subheading(c, title);
    titleCol.addView(titleView);
    topRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));

    topRow.addView(statusBadge(c, statusText, granted));
    body.addView(topRow);

    TextView desc = note(c, subtitle);
    desc.setPadding(0, dp(c, 10), 0, dp(c, 12));
    body.addView(desc);

    MaterialButton cta = granted
        ? tonalButton(c, actionText, action)
        : filledButton(c, actionText, action);
    body.addView(cta, new LinearLayout.LayoutParams(-1, dp(c, 48)));

    card.addView(body);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(c, 12);
    card.setLayoutParams(lp);
    return card;
  }

  static MaterialCardView settingTile(
      Context c, String title, String valueBadge, String subtitle, Runnable action) {
    MaterialCardView card = surfaceCard(c);
    card.setRadius(dp(c, 22));
    LinearLayout row = row(c);
    int px = dp(c, 16), py = dp(c, 14);
    row.setPadding(px, py, px, py);

    LinearLayout left = column(c);
    TextView t = text(c, title, 15, ink(c));
    t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    left.addView(t);
    if (subtitle != null && !subtitle.isEmpty()) {
      TextView sub = text(c, subtitle, 12, muted(c));
      sub.setPadding(0, dp(c, 2), 0, 0);
      left.addView(sub);
    }
    row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));

    if (valueBadge != null && !valueBadge.isEmpty()) {
      TextView pill = pillBadge(c, valueBadge, secondaryContainer(c), onSecondaryContainer(c));
      LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
      pp.leftMargin = dp(c, 8);
      row.addView(pill, pp);
    }

    card.addView(row);
    card.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(c, 8);
    card.setLayoutParams(lp);
    return card;
  }

  static TextInputEditText field(
      LinearLayout parent, String label, String value, boolean multiline) {
    Context c = parent.getContext();
    TextInputLayout wrapper =
        new TextInputLayout(c, null, com.google.android.material.R.attr.textInputOutlinedStyle);
    wrapper.setHint(label);
    float r = dp(c, 18);
    wrapper.setBoxCornerRadii(r, r, r, r);
    wrapper.setBoxBackgroundColor(surfaceLow(c));
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
    lp.bottomMargin = dp(c, 10);
    parent.addView(wrapper, lp);
    return e;
  }

  static String value(EditText e) {
    return e.getText() == null ? "" : e.getText().toString().trim();
  }

  static ScrollView scroll(Context c, View child) {
    ScrollView v = new ScrollView(c);
    v.setFillViewport(true);
    v.addView(child);
    return v;
  }

  static void inset(LinearLayout l, int amount) {
    int p = dp(l.getContext(), amount);
    l.setPadding(p, p, p, p);
  }

  static String day(int day) {
    return new String[] {"周一", "周二", "周三", "周四", "周五", "周六", "周日"}[day - 1];
  }

  static int courseColor(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xff4e2c3a, 0xff2c3663, 0xff1e4b45, 0xff543d1e, 0xff492e58, 0xff254457}
            : new int[] {0xffffd9e4, 0xffdee3ff, 0xffcbf2e9, 0xffffe5be, 0xfff1daff, 0xffd3ecff})
        [Math.floorMod(index, 6)];
  }

  static int courseAccent(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffff85ad, 0xff94abff, 0xff66decb, 0xffffb95c, 0xffdc9eff, 0xff76c8ff}
            : new int[] {0xffc83768, 0xff3657ce, 0xff137a6b, 0xffb86800, 0xff7b389e, 0xff166696})
        [Math.floorMod(index, 6)];
  }

  static int courseInk(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffffe8ef, 0xffe8ecff, 0xffe0faf4, 0xffffefda, 0xfff7e8ff, 0xffe2f3ff}
            : new int[] {0xff3d091d, 0xff091d65, 0xff05332b, 0xff3b2000, 0xff2e0942, 0xff062c42})
        [Math.floorMod(index, 6)];
  }
}
