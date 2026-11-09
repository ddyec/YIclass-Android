package com.ddyec.yiclass.importer;

import android.content.Context;
import android.net.Uri;
import com.ddyec.yiclass.core.PdfTableParser;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.*;
import java.io.*;
import java.util.*;

public final class PdfImporter {
  public PdfTableParser.Result read(Context context, Uri uri) throws IOException {
    byte[] data;
    try (InputStream in = context.getContentResolver().openInputStream(uri)) {
      if (in == null) throw new IOException("无法打开文件");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int n;
      while ((n = in.read(buffer)) != -1) {
        if (out.size() + n > 20 * 1024 * 1024) throw new IOException("PDF 超过20MB");
        out.write(buffer, 0, n);
      }
      data = out.toByteArray();
    }
    if (data.length > 20 * 1024 * 1024) throw new IOException("PDF 超过20MB，请精简后重试");
    List<PdfTableParser.Glyph> glyphs = new ArrayList<>();
    try (PDDocument document = PDDocument.load(data)) {
      if (document.getNumberOfPages() > 40) throw new IOException("最多支持40页 PDF");
      new PDFTextStripper() {
        @Override
        protected void processTextPosition(TextPosition t) {
          if (glyphs.size() > 300000) throw new IllegalArgumentException("PDF 文字过多");
          glyphs.add(
              new PdfTableParser.Glyph(
                  getCurrentPageNo(), t.getXDirAdj(), t.getYDirAdj(), t.getUnicode()));
        }
      }.getText(document);
    }
    return new PdfTableParser().parse(glyphs);
  }
}
