package com.ddyec.yiclass.ui;

import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import androidx.graphics.shapes.CornerRounding;
import androidx.graphics.shapes.RoundedPolygon;
import androidx.graphics.shapes.ShapesKt;
import androidx.graphics.shapes.Shapes_androidKt;

/**
 * Material 3 Expressive shape set (circle, cookies, sunny, burst, clover) built on the public
 * androidx.graphics.shapes API, normalized to a unit square so they scale to any view size.
 */
final class ExpressiveShapes {
  private ExpressiveShapes() {}

  static final RoundedPolygon CIRCLE = ShapesKt.circle(RoundedPolygon.Companion, 12);
  static final RoundedPolygon COOKIE_6 = star(6, 0.75f, 0.5f);
  static final RoundedPolygon COOKIE_9 = star(9, 0.8f, 0.5f);
  static final RoundedPolygon SUNNY = star(8, 0.8f, 0.15f);
  static final RoundedPolygon SOFT_BURST = star(10, 0.7f, 0.25f);
  static final RoundedPolygon CLOVER_4 = star(4, 0.45f, 0.42f);

  private static RoundedPolygon star(int vertices, float innerRadius, float rounding) {
    return ShapesKt.star(
        RoundedPolygon.Companion, vertices, 1f, innerRadius, new CornerRounding(rounding, 0f));
  }

  static ShapeDrawable drawable(RoundedPolygon polygon) {
    Path path = Shapes_androidKt.toPath(polygon.normalized(), new Path());
    return new ShapeDrawable(new PathShape(path, 1f, 1f));
  }
}
