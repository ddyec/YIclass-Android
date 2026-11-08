package com.ddyec.yiclass.ui;

import android.graphics.*;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.widget.*;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.*;

/** In-app page preview; respects the PDF page rotation through Android's renderer. */
final class PdfPreview {
  private final MainActivity a;
  private PdfRenderer renderer;
  private ParcelFileDescriptor fd;
  private File temporary;
  private Bitmap bitmap;
  private int index = 0;

  PdfPreview(MainActivity a) {
    this.a = a;
  }

  void show(Uri uri) {
    try {
      temporary = File.createTempFile("preview-", ".pdf", a.getCacheDir());
      try (InputStream in = a.getContentResolver().openInputStream(uri);
          OutputStream out = new FileOutputStream(temporary)) {
        if (in == null) throw new IOException("无法打开PDF");
        byte[] b = new byte[8192];
        int n, total = 0;
        while ((n = in.read(b)) != -1) {
          total += n;
          if (total > 20 * 1024 * 1024) throw new IOException("PDF超过20MB");
          out.write(b, 0, n);
        }
      }
      fd = ParcelFileDescriptor.open(temporary, ParcelFileDescriptor.MODE_READ_ONLY);
      renderer = new PdfRenderer(fd);
      LinearLayout box = Ui.column(a);
      ImageView image = new ImageView(a);
      image.setAdjustViewBounds(true);
      image.setBackgroundColor(Color.WHITE);
      TextView label = Ui.note(a, "");
      Runnable render =
          () -> {
            try (PdfRenderer.Page p = renderer.openPage(index)) {
              float scale = Math.min(1600f / p.getWidth(), 2400f / p.getHeight());
              Bitmap next =
                  Bitmap.createBitmap(
                      Math.max(1, (int) (p.getWidth() * scale)),
                      Math.max(1, (int) (p.getHeight() * scale)),
                      Bitmap.Config.ARGB_8888);
              next.eraseColor(Color.WHITE);
              p.render(next, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
              image.setImageBitmap(next);
              if (bitmap != null) bitmap.recycle();
              bitmap = next;
              label.setText("第 " + (index + 1) + " / " + renderer.getPageCount() + " 页");
            }
          };
      LinearLayout buttons = Ui.row(a);
      buttons.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 20), Ui.dp(a, 8));
      label.setGravity(android.view.Gravity.CENTER);
      buttons.addView(
          Ui.iconButton(
              a,
              com.ddyec.yiclass.R.drawable.ic_chevron_left,
              "上一页",
              true,
              () -> {
                if (index > 0) {
                  index--;
                  render.run();
                }
              }));
      buttons.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
      buttons.addView(
          Ui.iconButton(
              a,
              com.ddyec.yiclass.R.drawable.ic_chevron_right,
              "下一页",
              true,
              () -> {
                if (index + 1 < renderer.getPageCount()) {
                  index++;
                  render.run();
                }
              }));
      box.addView(buttons);
      com.google.android.material.card.MaterialCardView page = Ui.surfaceCard(a);
      page.setRadius(Ui.dp(a, 16));
      page.addView(Ui.scroll(a, image));
      LinearLayout.LayoutParams pageLp = new LinearLayout.LayoutParams(-1, -2);
      pageLp.setMargins(Ui.dp(a, 20), 0, Ui.dp(a, 20), 0);
      box.addView(page, pageLp);
      render.run();
      var dialog =
          new MaterialAlertDialogBuilder(a)
              .setIcon(com.ddyec.yiclass.R.drawable.ic_pdf)
              .setTitle("原始课表 PDF")
              .setView(box)
              .setPositiveButton("关闭", null)
              .create();
      dialog.setOnDismissListener(d -> close());
      dialog.show();
    } catch (Exception e) {
      close();
      a.error(e);
    }
  }

  private void close() {
    if (renderer != null) {
      renderer.close();
      renderer = null;
    }
    if (fd != null)
      try {
        fd.close();
      } catch (IOException ignored) {
      }
    if (bitmap != null) {
      bitmap.recycle();
      bitmap = null;
    }
    if (temporary != null) temporary.delete();
  }
}
