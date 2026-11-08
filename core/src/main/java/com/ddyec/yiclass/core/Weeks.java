package com.ddyec.yiclass.core;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

public final class Weeks {
  private Weeks() {}

  public static Set<Integer> parse(String input) {
    String s =
        Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replaceAll("\\s+", "")
            .replace("第", "")
            .replace("周", "")
            .replace("、", ",")
            .replace("，", ",")
            .replace("–", "-")
            .replace("~", "-");
    Set<Integer> result = new TreeSet<>();
    for (String part : s.split(",", -1)) {
      Matcher m = Pattern.compile("(\\d{1,2})(?:-(\\d{1,2}))?(?:\\(([单双])\\))?").matcher(part);
      if (!m.matches()) throw new IllegalArgumentException("周次格式示例：1-16、1-15(单)、2,4,6");
      int a = Integer.parseInt(m.group(1)),
          b = m.group(2) == null ? a : Integer.parseInt(m.group(2));
      if (a < 1 || b > 30 || a > b) throw new IllegalArgumentException("周次必须在1–30周内");
      for (int i = a; i <= b; i++)
        if (m.group(3) == null || (i % 2 == 1) == m.group(3).equals("单")) result.add(i);
    }
    if (result.isEmpty()) throw new IllegalArgumentException("周次不能为空");
    return result;
  }

  public static String canonical(String input) {
    return parse(input).toString();
  }
}
