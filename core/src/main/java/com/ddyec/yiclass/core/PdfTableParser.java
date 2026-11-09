package com.ddyec.yiclass.core;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

public final class PdfTableParser {
  public record Glyph(int page, float x, float y, String text) {}

  public record Result(List<Item> items, List<String> warnings) {}

  private static final Pattern SLOT =
      Pattern.compile("\\((\\d{1,2})-(\\d{1,2})节\\)(第?\\d{1,2}(?:-\\d{1,2})?周(?:\\([单双]\\))?)");

  public Result parse(List<Glyph> glyphs) {
    Map<Integer, List<Glyph>> pages = new TreeMap<>();
    for (Glyph g : glyphs)
      pages
          .computeIfAbsent(g.page(), k -> new ArrayList<>())
          .add(new Glyph(g.page(), g.x(), g.y(), normalize(g.text())));
    List<Item> items = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    Map<String, Integer> colorByTitle = new LinkedHashMap<>();
    float[] centers = null;
    float headerY = -1;
    boolean hasAppendix = false;
    for (var entry : pages.entrySet()) {
      List<List<Glyph>> lines = lines(entry.getValue());
      StringBuilder appendix = new StringBuilder();
      boolean inAppendix = false;
      for (List<Glyph> line : lines) {
        String text = join(line);
        if (text.contains("其他课程")) inAppendix = true;
        if (text.contains("打印时间")) break;
        if (inAppendix) appendix.append(text).append('\n');
      }
      if (appendix.length() > 0) {
        hasAppendix = true;
        warnings.add("以下原文信息没有固定节次，请核对后手动补充，未自动安排时间：\n" + appendix);
      }
      for (List<Glyph> line : lines) {
        String text = join(line);
        if (text.contains("星期一") && text.contains("星期日")) {
          centers = new float[7];
          String[] labels = {"一", "二", "三", "四", "五", "六", "日"};
          int offset = 0;
          for (int d = 0; d < 7; d++) {
            int idx = text.indexOf("星期" + labels[d], offset);
            if (idx < 0) throw new IllegalArgumentException("无法识别星期表头");
            centers[d] = charX(line, idx + 1);
            offset = idx + 3;
          }
          headerY = line.get(0).y();
        }
      }
      if (centers == null)
        throw new IllegalArgumentException("未识别到星期表头。请使用教务系统导出的文字版 PDF；扫描件暂不支持。");
      for (int day = 1; day <= 7; day++) {
        List<Glyph> col = new ArrayList<>();
        float left =
            day == 1
                ? centers[0] - (centers[1] - centers[0]) / 2
                : (centers[day - 2] + centers[day - 1]) / 2;
        float right =
            day == 7
                ? centers[6] + (centers[6] - centers[5]) / 2
                : (centers[day - 1] + centers[day]) / 2;
        for (Glyph g : entry.getValue())
          if (g.x() >= left
              && g.x() < right
              && (entry.getKey() != pages.keySet().iterator().next() || g.y() > headerY + 2))
            col.add(g);
        List<List<Glyph>> columnLines = lines(col);
        StringBuilder text = new StringBuilder();
        for (var line : columnLines) text.append(join(line).trim()).append('\n');
        parseColumn(text.toString(), day, items, warnings, colorByTitle);
      }
    }
    if (items.isEmpty()) throw new IllegalArgumentException("PDF 中没有可识别的定时课程，请检查格式或手动录入。");
    if (!hasAppendix && glyphs.stream().anyMatch(g -> g.text().contains("其")))
      warnings.add("PDF 中的其他课程/网课没有固定节次，请核对原 PDF 并在日程中补充。");
    warnings.add("PDF 不包含每节课的钟点：当前使用08:30起的默认作息，请在设置核对。学期首周日期也需确认。");
    return new Result(items, warnings);
  }

  private static void parseColumn(
      String text,
      int day,
      List<Item> out,
      List<String> warnings,
      Map<String, Integer> colorByTitle) {
    Matcher m = SLOT.matcher(text);
    int lastEnd = 0;
    List<int[]> positions = new ArrayList<>();
    List<String[]> fields = new ArrayList<>();
    while (m.find()) {
      positions.add(new int[] {m.start(), m.end()});
      fields.add(new String[] {m.group(1), m.group(2), m.group(3)});
    }
    for (int i = 0; i < positions.size(); i++) {
      int begin = positions.get(i)[0], end = positions.get(i)[1];
      String prior = text.substring(lastEnd, begin);
      StringBuilder title = new StringBuilder();
      String[] ls = prior.trim().split("\\n");
      for (int j = ls.length - 1; j >= 0; j--) {
        String l = ls[j].trim();
        if (l.isEmpty()) continue;
        if (l.contains("/") || l.contains(":") || l.matches("[0-9.]+") || l.contains("其他课程")) break;
        title.insert(0, l);
      }
      int stop = i + 1 < positions.size() ? positions.get(i + 1)[0] : text.length();
      String metadata = text.substring(end, stop).replaceAll("\\s+", "");
      int a = Integer.parseInt(fields.get(i)[0]), b = Integer.parseInt(fields.get(i)[1]);
      if (title.length() == 0 || a < 1 || b > 10 || a > b) {
        warnings.add("星期" + day + " 有未识别的课程，请核对原文");
        lastEnd = end;
        continue;
      }
      Item c = new Item();
      c.title = title.toString();
      c.day = day;
      c.start = Schedule.STARTS[a - 1];
      c.end = Schedule.ENDS[b - 1];
      c.weeks = fields.get(i)[2];
      c.location = field(metadata, "场地");
      c.teacher = field(metadata, "教师");
      Matcher total = Pattern.compile("课程总学时:\\d+(?:\\.\\d+)?").matcher(metadata);
      String original = total.find() ? metadata.substring(0, total.end()) : metadata;
      c.notes = "由本地 PDF 导入\n第" + a + "–" + b + "节 · " + c.weeks + "\n" + original;
      c.color = colorByTitle.computeIfAbsent(c.title, k -> colorByTitle.size() % 6);
      c.validate();
      if (out.stream().noneMatch(x -> x.fingerprint().equals(c.fingerprint()))) out.add(c);
      lastEnd = end;
    }
  }

  private static String field(String text, String key) {
    Matcher m = Pattern.compile(Pattern.quote(key) + ":([^/]+)").matcher(text);
    return m.find() ? m.group(1) : "";
  }

  public static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFKC)
        .replace('（', '(')
        .replace('）', ')')
        .replace('：', ':');
  }

  private static List<List<Glyph>> lines(List<Glyph> source) {
    List<Glyph> sorted = new ArrayList<>(source);
    sorted.sort(Comparator.comparingDouble(Glyph::y).thenComparingDouble(Glyph::x));
    List<List<Glyph>> result = new ArrayList<>();
    for (Glyph g : sorted) {
      if (result.isEmpty() || Math.abs(g.y() - result.get(result.size() - 1).get(0).y()) > 2.2f)
        result.add(new ArrayList<>());
      result.get(result.size() - 1).add(g);
    }
    for (var l : result) l.sort(Comparator.comparingDouble(Glyph::x));
    return result;
  }

  private static String join(List<Glyph> line) {
    StringBuilder b = new StringBuilder();
    for (Glyph g : line) b.append(g.text());
    return b.toString();
  }

  private static float charX(List<Glyph> line, int index) {
    int count = 0;
    for (Glyph g : line) {
      count += g.text().length();
      if (count > index) return g.x();
    }
    return line.get(line.size() - 1).x();
  }
}
