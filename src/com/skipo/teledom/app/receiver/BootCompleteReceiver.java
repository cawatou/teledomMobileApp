package com.skipo.teledom.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootCompleteReceiver extends BroadcastReceiver {
  public BootCompleteReceiver() {
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    if ( action.equals("android.intent.action.BOOT_COMPLETED") ||
         action.equals("android.intent.action.QUICKBOOT_POWERON") ||
         action.equals("com.htc.intent.action.QUICKBOOT_POWERON") ) {

      Toast.makeText(context, "TELEDOM BOOT UP", Toast.LENGTH_LONG).show();
    }

  }

}
