package com.ddyec.yiclass.reminder;

import android.content.*;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    PendingResult pending = goAsync();
    new Thread(
            () -> {
              try {
                Reminders.deliver(c, intent.getLongExtra("at", 0));
                Reminders.refresh(c);
              } finally {
                pending.finish();
              }
            },
            "reminders")
        .start();
  }
}
