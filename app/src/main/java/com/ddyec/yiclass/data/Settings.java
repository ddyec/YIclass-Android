package com.ddyec.yiclass.data;

import android.content.*;
import com.ddyec.yiclass.core.Schedule;
import java.time.*;

public final class Settings {
  private final SharedPreferences prefs;

  public Settings(Context c) {
    prefs = c.getSharedPreferences("preferences", Context.MODE_PRIVATE);
  }

  public String get(String key, String fallback) {
    return prefs.getString(key, fallback);
  }

  public void put(String key, String value) {
    prefs.edit().putString(key, value).apply();
  }

  public LocalDate term() {
    return LocalDate.parse(
        get("term", LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString()));
  }

  public ZoneId zone() {
    return ZoneId.of(get("zone", "Asia/Shanghai"));
  }

  public int theme() {
    return Integer.parseInt(get("theme", "-1"));
  }

  public boolean dynamicColor() {
    return "1".equals(get("dynamic_color", "0"));
  }

  public String model() {
    String m = get("model", "deepseek-flash").trim();
    if (m.isEmpty() || "deepseek-chat".equals(m) || "deepseek-v4-flash".equals(m)) {
      return "deepseek-flash";
    }
    if ("deepseek-reasoner".equals(m)) {
      return "deepseek-v4-pro";
    }
    return m;
  }

  public boolean thinking() {
    return "1".equals(get("thinking", "0"));
  }

  public int[] starts() {
    return slots("starts", Schedule.STARTS);
  }

  public int[] ends() {
    return slots("ends", Schedule.ENDS);
  }

  private int[] slots(String key, int[] fallback) {
    String s = get(key, "");
    if (s.isEmpty()) return fallback.clone();
    String[] parts = s.split(",");
    int[] a = new int[parts.length];
    for (int i = 0; i < a.length; i++) a[i] = Integer.parseInt(parts[i]);
    return a;
  }
}
