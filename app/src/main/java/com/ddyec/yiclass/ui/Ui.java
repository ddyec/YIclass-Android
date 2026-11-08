package com.ddyec.yiclass.ui;

import android.content.Context;
import android.graphics.Typeface;
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

  static int ink(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnSurface);
  }

  static int muted(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnSurfaceVariant);
  }

  static int primary(Context c) {
    return color(c, com.google.android.material.R.attr.colorPrimary);
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
    TextView t = text(c, s, 24, ink(c));
    t.setTypeface(null, Typeface.BOLD);
    t.setPadding(0, dp(c, 12), 0, dp(c, 12));
    return t;
  }

  static TextView note(Context c, String s) {
    TextView t = text(c, s, 14, muted(c));
    t.setLineSpacing(dp(c, 4), 1);
    t.setPadding(0, dp(c, 8), 0, dp(c, 8));
    return t;
  }

  static MaterialButton button(Context c, String text, Runnable action) {
    MaterialButton b =
        new MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
    b.setText(text);
    b.setMinHeight(dp(c, 48));
    b.setMinWidth(dp(c, 48));
    b.setPadding(dp(c, 12), 0, dp(c, 12), 0);
    b.setMaxLines(text.contains("\n") ? 2 : 1);
    b.setEllipsize(android.text.TextUtils.TruncateAt.END);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  static MaterialCardView card(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 20));
    v.setCardElevation(0);
    v.setStrokeWidth(dp(c, 1));
    v.setStrokeColor(color(c, com.google.android.material.R.attr.colorOutlineVariant));
    return v;
  }

  static TextInputEditText field(
      LinearLayout parent, String label, String value, boolean multiline) {
    Context c = parent.getContext();
    TextInputLayout wrapper =
        new TextInputLayout(c, null, com.google.android.material.R.attr.textInputOutlinedStyle);
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
    boolean dark = (c.getResources().getConfiguration().uiMode & 48) == 32;
    return (dark
            ? new int[] {0xff49323d, 0xff343959, 0xff284c48, 0xff534127, 0xff493553, 0xff344752}
            : new int[] {0xfff7dfe7, 0xffe2e5ff, 0xffd4eee8, 0xffffe9c6, 0xffeeddf5, 0xffdfecf7})
        [Math.floorMod(index, 6)];
  }
}
