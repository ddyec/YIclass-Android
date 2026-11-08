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

/**
 * Programmatic Material 3 Expressive toolkit: emphasized typography, Expressive buttons with
 * shape morphing, connected button groups, segmented lists and Expressive shape containers.
 */
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

  // ---- Color roles -------------------------------------------------------------------------

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

  static int errorContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorErrorContainer);
  }

  static int onErrorContainer(Context c) {
    return color(c, com.google.android.material.R.attr.colorOnErrorContainer);
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

  // ---- Layout ------------------------------------------------------------------------------

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

  static View space(Context c, int dp) {
    View v = new View(c);
    v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, dp), dp(c, dp)));
    return v;
  }

  // ---- Typography (emphasized Expressive type) ----------------------------------------------

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

  /** Emphasized weight: variable weight on API 28+, bold below. */
  static void emphasize(TextView t, int weight) {
    if (Build.VERSION.SDK_INT >= 28)
      t.setTypeface(Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), weight, false));
    else t.setTypeface(Typeface.create("sans-serif-medium", weight >= 600 ? Typeface.BOLD : Typeface.NORMAL));
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
    t.setPadding(dp(c, 16), dp(c, 22), dp(c, 16), dp(c, 10));
    return t;
  }

  // ---- Shapes ------------------------------------------------------------------------------

  static GradientDrawable roundedBg(
      Context c, int fill, float radiusDp, int strokeWidthDp, int strokeColor) {
    GradientDrawable d = new GradientDrawable();
    d.setShape(GradientDrawable.RECTANGLE);
    d.setColor(fill);
    d.setCornerRadius(dp(c, radiusDp));
    if (strokeWidthDp > 0) d.setStroke(dp(c, strokeWidthDp), strokeColor);
    return d;
  }

  /** A Material 3 Expressive shape (cookie, sunny, clover, ...) filled with a color role. */
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

  /** Icon centered on an Expressive shape, e.g. the hero / empty-state / leading-list marks. */
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

  /** Expressive segmented-list shape: large outer corners, tight inner corners. */
  static ShapeAppearanceModel segment(Context c, int position, int count) {
    float outer = 24, inner = 6;
    boolean first = position == 0, last = position == count - 1;
    return corners(c, first ? outer : inner, last ? outer : inner);
  }

  // ---- Badges ------------------------------------------------------------------------------

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

  // ---- Buttons (Expressive: shape morph on press, size overlays) ---------------------------

  private static MaterialButton make(Context c, int styleAttr, String text, Runnable action) {
    MaterialButton b = new MaterialButton(c, null, styleAttr);
    b.setText(text);
    b.setAllCaps(false);
    b.setMaxLines(text.contains("\n") ? 2 : 1);
    b.setEllipsize(TextUtils.TruncateAt.END);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  /** Outlined button – low emphasis. */
  static MaterialButton button(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonOutlinedStyle, text, action);
  }

  /** Text button – lowest emphasis. */
  static MaterialButton textButton(Context c, String text, Runnable action) {
    return make(c, androidx.appcompat.R.attr.borderlessButtonStyle, text, action);
  }

  /** Filled tonal – medium emphasis. */
  static MaterialButton tonalButton(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonTonalStyle, text, action);
  }

  /** Filled – high emphasis. */
  static MaterialButton filledButton(Context c, String text, Runnable action) {
    return make(c, com.google.android.material.R.attr.materialButtonStyle, text, action);
  }

  /** Medium (56dp) filled – hero call to action. */
  static MaterialButton heroButton(Context c, String text, Runnable action) {
    return make(c, R.attr.yiButtonFilledMedium, text, action);
  }

  /** Medium (56dp) tonal. */
  static MaterialButton heroTonalButton(Context c, String text, Runnable action) {
    return make(c, R.attr.yiButtonTonalMedium, text, action);
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
    TooltipCompatHolder.set(b, description);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  static MaterialButton mediumIconButton(
      Context c, int iconRes, String description, Runnable action) {
    MaterialButton b = new MaterialButton(c, null, R.attr.yiIconButtonTonalMedium);
    b.setIconResource(iconRes);
    b.setContentDescription(description);
    TooltipCompatHolder.set(b, description);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private static final class TooltipCompatHolder {
    static void set(View v, String text) {
      androidx.appcompat.widget.TooltipCompat.setTooltipText(v, text);
    }
  }

  /**
   * Expressive connected button group (replaces segmented buttons). Selected item morphs to the
   * checked shape and switches to the primary container.
   */
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

  // ---- Containers --------------------------------------------------------------------------

  static MaterialCardView card(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 16));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(surfaceLow(c));
    return v;
  }

  static MaterialCardView heroCard(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 32));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(primaryContainer(c));
    return v;
  }

  static MaterialCardView surfaceCard(Context c) {
    MaterialCardView v = new MaterialCardView(c);
    v.setRadius(dp(c, 28));
    v.setCardElevation(0);
    v.setStrokeWidth(0);
    v.setCardBackgroundColor(surfaceContainer(c));
    return v;
  }

  /** One row of an Expressive segmented list (grouped with 2dp gaps). */
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
    row.setMinimumHeight(dp(c, 72));
    row.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
    if (iconRes != 0) {
      FrameLayout lead =
          shapeIcon(
              c,
              ExpressiveShapes.CIRCLE,
              iconRes,
              secondaryContainer(c),
              onSecondaryContainer(c),
              40,
              22);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40));
      lp.rightMargin = dp(c, 16);
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
    TextView t = styled(c, value, com.google.android.material.R.attr.textAppearanceLabelLarge, primary(c));
    t.setMaxWidth(dp(c, 140));
    t.setSingleLine(true);
    t.setEllipsize(TextUtils.TruncateAt.END);
    return t;
  }

  /** Prominent permission banner: Expressive shape mark, status badge and full-width CTA. */
  static MaterialCardView permissionCard(
      Context c,
      int iconRes,
      String title,
      String subtitle,
      String statusText,
      boolean granted,
      String actionText,
      Runnable action) {
    MaterialCardView card = new MaterialCardView(c);
    card.setRadius(dp(c, 28));
    card.setCardElevation(0);
    card.setStrokeWidth(0);
    card.setCardBackgroundColor(granted ? surfaceContainer(c) : tertiaryContainer(c));
    int fg = granted ? ink(c) : onTertiaryContainer(c);

    LinearLayout body = column(c);
    inset(body, 20);
    LinearLayout topRow = row(c);
    topRow.addView(
        shapeIcon(
            c,
            ExpressiveShapes.SOFT_BURST,
            iconRes,
            granted ? secondaryContainer(c) : tertiary(c),
            granted
                ? onSecondaryContainer(c)
                : color(c, com.google.android.material.R.attr.colorOnTertiary),
            48,
            24));
    LinearLayout titleCol = column(c);
    titleCol.setPadding(dp(c, 14), 0, dp(c, 8), 0);
    TextView titleView = subheading(c, title);
    titleView.setTextColor(fg);
    titleCol.addView(titleView);
    topRow.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
    topRow.addView(
        granted
            ? statusBadge(c, statusText, true)
            : pillBadge(
                c,
                statusText,
                tertiary(c),
                color(c, com.google.android.material.R.attr.colorOnTertiary)));
    body.addView(topRow);

    TextView desc = note(c, subtitle);
    desc.setTextColor(fg);
    desc.setAlpha(granted ? 1f : 0.86f);
    desc.setPadding(0, dp(c, 12), 0, dp(c, 14));
    body.addView(desc);

    MaterialButton cta =
        granted ? tonalButton(c, actionText, action) : filledButton(c, actionText, action);
    body.addView(cta, new LinearLayout.LayoutParams(-1, dp(c, 48)));
    card.addView(body);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(c, 12);
    card.setLayoutParams(lp);
    return card;
  }

  /** Expressive empty state: large cookie shape mark, emphasized headline and a medium CTA. */
  static View emptyState(
      Context c, int iconRes, String title, String note, String button, int buttonIcon, Runnable action) {
    MaterialCardView card = surfaceCard(c);
    card.setRadius(dp(c, 32));
    LinearLayout box = column(c);
    box.setPadding(dp(c, 24), dp(c, 28), dp(c, 24), dp(c, 24));
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    FrameLayout mark =
        shapeIcon(
            c, ExpressiveShapes.COOKIE_9, iconRes, primaryContainer(c), onPrimaryContainer(c), 112, 48);
    box.addView(mark);
    TextView h = heading(c, title);
    h.setGravity(Gravity.CENTER);
    h.setPadding(0, dp(c, 18), 0, dp(c, 4));
    box.addView(h);
    TextView message = note(c, note);
    message.setGravity(Gravity.CENTER);
    box.addView(message);
    MaterialButton cta = withIcon(heroButton(c, button, action), buttonIcon);
    LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
    bp.topMargin = dp(c, 12);
    box.addView(cta, bp);
    card.addView(box);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(c, 8);
    card.setLayoutParams(lp);
    return card;
  }

  // ---- Text fields -------------------------------------------------------------------------

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

  // ---- Domain helpers ----------------------------------------------------------------------

  static String day(int day) {
    return new String[] {"周一", "周二", "周三", "周四", "周五", "周六", "周日"}[day - 1];
  }

  static int courseColor(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xff5b2a3c, 0xff2c3a76, 0xff1d4d45, 0xff58401b, 0xff4c2d63, 0xff1f4660}
            : new int[] {0xffffd9e2, 0xffdde1ff, 0xffc4efe3, 0xffffdeaf, 0xfff3daff, 0xffcde5ff})
        [Math.floorMod(index, 6)];
  }

  static int courseAccent(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffffb1c6, 0xffb8c4ff, 0xff8fd5c3, 0xfff2bf69, 0xffe2b7ff, 0xff96cbf5}
            : new int[] {0xffa83c5d, 0xff3254cb, 0xff1b6b5d, 0xff815500, 0xff7b4699, 0xff1e6390})
        [Math.floorMod(index, 6)];
  }

  static int courseInk(Context c, int index) {
    return (isDark(c)
            ? new int[] {0xffffd9e2, 0xffdde1ff, 0xffc4efe3, 0xffffdeaf, 0xfff3daff, 0xffcde5ff}
            : new int[] {0xff3f0a1e, 0xff0d2a8a, 0xff00382f, 0xff2a1800, 0xff2f0848, 0xff001d33})
        [Math.floorMod(index, 6)];
  }
}
