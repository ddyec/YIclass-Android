package com.ddyec.yiclass.core;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;
import org.junit.Test;

public class PdfTableParserTest {
  private void line(List<PdfTableParser.Glyph> glyphs, int page, float x, float y, String text) {
    int n = 0;
    for (char c : text.toCharArray())
      glyphs.add(new PdfTableParser.Glyph(page, x + 4 * n++, y, String.valueOf(c)));
  }

  @Test
  public void readsColumnsUnicodeParityAndStripsDiamonds() {
    List<PdfTableParser.Glyph> glyphs = new ArrayList<>();
    String[] names = {"一", "二", "三", "四", "五", "六", "⽇"};
    for (int d = 0; d < 7; d++) line(glyphs, 1, 100 + d * 110, 60, "星期" + names[d]);
    line(glyphs, 1, 320, 90, "⼤学体育（篮球1）\uFFFD◆◇★");
    line(glyphs, 1, 320, 110, "(5-6节)4-15周/校区:本部");
    line(glyphs, 1, 320, 125, "/场地:体育馆\uFFFD/教师:教师甲◇/教学班:甲");
    line(glyphs, 2, 320, 80, "晚间课程◆");
    line(glyphs, 2, 320, 100, "(9-10节)5-11周(单)/场地:教室/教师:乙/");
    var result = new PdfTableParser().parse(glyphs);
    assertEquals(2, result.items().size());
    Item basketball = result.items().get(0);
    assertEquals(3, basketball.day);
    assertEquals("大学体育(篮球1)", basketball.title);
    assertEquals("体育馆", basketball.location);
    assertEquals("教师甲", basketball.teacher);
    assertEquals("", basketball.notes);
    assertEquals(Set.of(5, 7, 9, 11), Weeks.parse(result.items().get(1).weeks));
  }

  @Test
  public void termDateDriftsAutomaticallyToMonday() {
    assertEquals(LocalDate.of(2026, 8, 31), Schedule.parseTermMonday("09-03", 2026));
    assertEquals(LocalDate.of(2026, 8, 31), Schedule.parseTermMonday("2026-09-03", 2026));
    assertEquals(LocalDate.of(2026, 8, 31), Schedule.parseTermMonday("2026/9/6", 2026));
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsImageOrUnrecognizedTableInsteadOfInventingDays() {
    new PdfTableParser().parse(List.of());
  }
}
