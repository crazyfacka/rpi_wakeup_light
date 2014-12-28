package com.hairysoft.cockcrow;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import com.hairysoft.util.Log;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Fragment to show the Alarm time picker
 */
public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private final static String TAG = "TimePickerFragment";

    private OnAlarmSelectedListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAlarmSelectedListener)activity;
        } catch(ClassCastException ex) {
            Log.e(TAG, activity.toString() + " hasn't implemented the listener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Log.d(TAG, "Alarm chosen to ring @ " + hourOfDay + ":" + minute);
        mListener.onAlarmSelected(hourOfDay, minute);
    }

    public static interface OnAlarmSelectedListener {
        public abstract void onAlarmSelected(int hourOfDay, int minute);
    }

}
