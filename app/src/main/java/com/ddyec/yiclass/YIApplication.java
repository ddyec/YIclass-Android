package com.ddyec.yiclass;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.ddyec.yiclass.data.Settings;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class YIApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    PDFBoxResourceLoader.init(this);
    AppCompatDelegate.setDefaultNightMode(new Settings(this).theme());
    // Material 3 Expressive dynamic color (Android 12+), opt-in from Settings.
    DynamicColors.applyToActivitiesIfAvailable(
        this,
        new DynamicColorsOptions.Builder()
            .setPrecondition((activity, theme) -> new Settings(activity).dynamicColor())
            .build());
  }
}
