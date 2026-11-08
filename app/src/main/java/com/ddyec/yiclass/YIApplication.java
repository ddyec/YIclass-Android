package com.ddyec.yiclass;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.ddyec.yiclass.data.Settings;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class YIApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    PDFBoxResourceLoader.init(this);
    AppCompatDelegate.setDefaultNightMode(new Settings(this).theme());
  }
}
