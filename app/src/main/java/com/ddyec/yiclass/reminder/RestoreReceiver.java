package com.ddyec.yiclass.reminder;

import android.content.*;

public class RestoreReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    String action = intent.getAction();
    if (action == null
        || !java.util.Set.of(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")
            .contains(action)) return;
    PendingResult pending = goAsync();
    new Thread(
            () -> {
              try {
                Reminders.refresh(c);
              } finally {
                pending.finish();
              }
            },
            "restore-reminders")
        .start();
  }
}
