package com.skipo.teledom.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.skipo.teledom.app.service.CallDetachService;
import com.skipo.teledom.app.util.Constants;


import java.lang.reflect.Method;

import static android.content.Context.MODE_PRIVATE;

public class CallStateReceiver extends BroadcastReceiver {
    private static final String TAG = "[MyApp-CSR]";

    private SharedPreferences mSharedPreferences;
    private String callCenterNumber;
    private boolean callEnabled;

    public CallStateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "*********************************onReceive*******************************************");

        mSharedPreferences = context.getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);
        callEnabled = mSharedPreferences.getBoolean(Constants.CALL_ENABLED, true);
        callCenterNumber = "+" + mSharedPreferences.getString(Constants.CALL_CENTER_NUMBER, "jopa");

        if ( intent.getAction().equals("android.intent.action.PHONE_STATE") ) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number =  intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Log.i(TAG, "state: " + state + "\nphone: " + number);
            Toast.makeText(context, "state: " + state + "\nphone: " + number, Toast.LENGTH_SHORT).show();

            if ( state.equals(TelephonyManager.EXTRA_STATE_RINGING) && number.equals(callCenterNumber) ) {

                endCallIfBlocked(context);

                if  ( !callEnabled ) {
                    Log.i(TAG, "Call set disabled");
                    Toast.makeText(context, "Call set disabled", Toast.LENGTH_SHORT).show();
                    return;
                }

                context.startService(new Intent(context, CallDetachService.class));
                Log.i(TAG, "Start CallDetachService");

            }

        }

    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void endCallIfBlocked(Context context) {
        Log.i(TAG, "endCallIfBlocked");

      ITelephony telephonyService;
      TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

      try  {
        Class c = Class.forName(telephony.getClass().getName());
        Method m = c.getDeclaredMethod("getITelephony");
        m.setAccessible(true);
        telephonyService = (ITelephony) m.invoke(telephony);
        telephonyService.endCall();

      } catch (Exception e) {
        e.printStackTrace();

      }

    }


}
