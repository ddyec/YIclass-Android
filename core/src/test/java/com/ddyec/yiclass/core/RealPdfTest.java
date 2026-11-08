package com.ddyec.yiclass.core;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.*;
import org.junit.*;

public class RealPdfTest {
  @Test
  public void importsUserPdfIncludingSecondPageAndParity() throws Exception {
    String path = System.getProperty("fixture.pdf", "");
    Assume.assumeFalse("Pass -Dfixture.pdf to validate the private source PDF", path.isEmpty());
    List<PdfTableParser.Glyph> glyphs = new ArrayList<>();
    try (PDDocument doc = PDDocument.load(new File(path))) {
      new PDFTextStripper() {
        @Override
        protected void processTextPosition(TextPosition p) {
          glyphs.add(
              new PdfTableParser.Glyph(
                  getCurrentPageNo(), p.getXDirAdj(), p.getYDirAdj(), p.getUnicode()));
        }
      }.getText(doc);
    }
    var result = new PdfTableParser().parse(glyphs);
    for (Item i : result.items())
      System.out.println(
          i.day
              + " "
              + i.title
              + " "
              + Schedule.time(i.start)
              + "-"
              + Schedule.time(i.end)
              + " "
              + i.weeks
              + " "
              + i.location
              + " "
              + i.teacher);
    assertEquals(21, result.items().size());
    assertTrue(
        result.items().stream()
            .anyMatch(i -> i.start == 1140 && Weeks.parse(i.weeks).equals(Set.of(5, 7, 9, 11))));
    assertTrue(
        result.items().stream()
            .anyMatch(
                i ->
                    i.day == 5
                        && i.title.equals("普通化学II")
                        && Weeks.parse(i.weeks).equals(Set.of(4, 6, 8, 10, 12))));
    assertTrue(
        result.items().stream()
            .anyMatch(i -> i.day == 6 && Weeks.parse(i.weeks).equals(Set.of(9)) && i.end == 750));
    assertTrue(
        result.items().stream()
            .anyMatch(i -> i.title.equals("大学英语(一)") && i.teacher.equals("郭梦莹")));
    Item basketball =
        result.items().stream().filter(i -> i.title.contains("篮球")).findFirst().orElseThrow();
    // Visually verified against page 1, under the actual 星期三 header, not the reference screenshot.
    assertEquals(3, basketball.day);
    assertEquals(840, basketball.start);
    assertEquals(940, basketball.end);
    assertEquals(Weeks.parse("4-15"), Weeks.parse(basketball.weeks));
    assertEquals("陈生琛", basketball.teacher);
    // /Rotate is independent of the direction of the embedded glyphs. Never infer weekday
    // from PDF media-box coordinates or a hardcoded sequence of text tokens.
    List<String> baseline = result.items().stream().map(Item::fingerprint).sorted().toList();
    for (int rotation : new int[] {0, 90, 180, 270}) {
      List<PdfTableParser.Glyph> rotated = new ArrayList<>();
      try (PDDocument doc = PDDocument.load(new File(path))) {
        for (var page : doc.getPages()) page.setRotation(rotation);
        new PDFTextStripper() {
          @Override
          protected void processTextPosition(TextPosition p) {
            rotated.add(
                new PdfTableParser.Glyph(
                    getCurrentPageNo(), p.getXDirAdj(), p.getYDirAdj(), p.getUnicode()));
          }
        }.getText(doc);
      }
      assertEquals(
          "Page rotation " + rotation,
          baseline,
          new PdfTableParser()
              .parse(rotated).items().stream().map(Item::fingerprint).sorted().toList());
    }
    assertEquals(
        13,
        Schedule.on(result.items(), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 8, 31)).size()
            + Schedule.on(result.items(), LocalDate.of(2026, 11, 3), LocalDate.of(2026, 8, 31))
                .size()
            + Schedule.on(result.items(), LocalDate.of(2026, 11, 4), LocalDate.of(2026, 8, 31))
                .size()
            + Schedule.on(result.items(), LocalDate.of(2026, 11, 5), LocalDate.of(2026, 8, 31))
                .size()
            + Schedule.on(result.items(), LocalDate.of(2026, 11, 6), LocalDate.of(2026, 8, 31))
                .size());
  }
}
