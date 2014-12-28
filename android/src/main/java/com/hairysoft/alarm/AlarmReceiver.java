package com.hairysoft.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.hairysoft.util.Log;

/**
 * BroadcastReceiver for the Alarm set in the main application.
 * Starts the AlarmActivity upon receiving the broadcast.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm!");

        Intent i = new Intent(context, AlarmActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

    }

}
