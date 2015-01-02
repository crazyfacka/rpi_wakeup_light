package com.hairysoft.cockcrow;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
// import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.hairysoft.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AnalogClock;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
// import android.widget.Toast;

import com.hairysoft.alarm.AlarmActivity;
import com.hairysoft.alarm.AlarmReceiver;
import com.hairysoft.bt.ConnectThread;
import com.hairysoft.bt.Dispatcher;
import com.hairysoft.message.BaseMessage;
import com.hairysoft.message.ClockMessage;
import com.hairysoft.message.DemoSunrise;
import com.hairysoft.message.SetAlarm;
import com.hairysoft.message.SetBrightness;
import com.hairysoft.message.TurnOnOff;
import com.hairysoft.util.Constants;

import org.json.JSONException;

import java.util.Calendar;

/**
 * Main activity for the application
 */
public class MainActivity extends Activity implements TimePickerFragment.OnAlarmSelectedListener {

    private final static String TAG = "MainActivity";

    // The views used with a global scope
    private AnalogClock analogClock;
    private TextView alarmTimeView;
    private TextView alarmButtonText;
    private TextView connStatus;

    // Preferences
    private String alarmTime;
    private String notySoundUri;
    private SharedPreferences prefs;

    // Bluetooth connection handlers
    private BluetoothAdapter mBluetoothAdapter;
    private WakyBluetoothService wakyBluetoothService;

    // Alarm handlers
    private AlarmManager mAlarmManager;
    private Intent mNotificationReceiverIntent;
    private PendingIntent mNotificationReceiverPendingIntent;

    private boolean sliderAnimating = false;

    // Handler to receive notifications from other places within the application, and show/handle them in the UI thread
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Context c = getApplicationContext();
            switch (msg.what) {
                case Constants.BT_CONNECT_SUCCESS:
                    // Toast.makeText(c, R.string.connect_success, Toast.LENGTH_LONG).show();
                    connStatus.setText(R.string.connected);
                    break;
                case Constants.BT_CONNECT_FAILED:
                    // Toast.makeText(c, R.string.connect_failed, Toast.LENGTH_LONG).show();
                    connStatus.setText(R.string.cant_connect);
                    wakyBluetoothService.discoverWaky(true);
                    break;
                case Constants.BT_DISCONNECT:
                    connStatus.setText(R.string.disconnected);
                    wakyBluetoothService.discoverWaky(true);
                    break;
                case Constants.UPDATE_ALARM:
                    if(alarmTime != null && alarmTime.length() > 0) {
                        String[] items = alarmTime.split(":");
                        int hourOfDay = Integer.parseInt(items[0]);
                        int minute = Integer.parseInt(items[1]);
                        try {
                            Dispatcher.queueMessage(new SetAlarm(hourOfDay, minute).getJSON());
                        } catch(JSONException ex) { }
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initApp();
        initGUI();

        // Register the types of messages to be recognized by the application
        // More information in BaseMessage.java
        BaseMessage.registerClass(ClockMessage.class,
                SetBrightness.class,
                DemoSunrise.class,
                SetAlarm.class,
                TurnOnOff.class);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        wakyBluetoothService = new WakyBluetoothService(this, mHandler);
        Dispatcher.init();

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNotificationReceiverIntent = new Intent(this, AlarmReceiver.class);
        mNotificationReceiverPendingIntent = PendingIntent.getBroadcast(this, 0, mNotificationReceiverIntent, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!ConnectThread.isConnected()) {

            if (mBluetoothAdapter == null) { // This should not happen, though
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, Constants.AR_REQUEST_ENABLE_BT);
            } else {
                wakyBluetoothService.discoverWaky();
            }

        } else {
            connStatus.setText(R.string.connected);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        wakyBluetoothService.holdWaky();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.AR_REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK) {
                    wakyBluetoothService.discoverWaky();
                } else {
                    Log.d(TAG, "BT not enabled");
                    // Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_LONG).show();
                }
                break;
            case Constants.AR_SELECT_ALARMTONE:
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if(uri != null) {
                    String name = RingtoneManager.getRingtone(this, uri).getTitle(this);
                    Log.d(TAG, "Selected sound: " + name + " (" + uri.toString() + ")");

                    alarmButtonText.setText(name);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("noty_sound_uri", uri.toString());
                    editor.putString("noty_sound_name", name);
                    editor.commit();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_add_alarm) {
            Log.d(TAG, "Adding alarm");
            DialogFragment timePicker = new TimePickerFragment();
            timePicker.show(getFragmentManager(), "timePicker");
            return true;
        } else if(id == R.id.action_search_waky) {
            Log.d(TAG, "Searching for waky");
            if(!ConnectThread.isConnected()) {
                wakyBluetoothService.discoverWaky(true);
            }
            return true;
        } else if(id == R.id.action_demo) {
            Log.d(TAG, "Demoing sunrise");
            try {
                Dispatcher.queueMessage(new DemoSunrise().getJSON());
            } catch(JSONException ex) { }
            return true;
        } else if(id == R.id.action_demo_alarm) {
            startActivity(new Intent(this, AlarmActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAlarmSelected(int hourOfDay, int minute) {
        String timeString = hourOfDay+":"+(minute < 10 ? "0" : "")+minute;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("alarm_time", timeString);
        editor.commit();

        alarmTimeView.setText(timeString);
        alarmTime = timeString;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);

        if(c.getTimeInMillis() < System.currentTimeMillis()) {
            c.add(Calendar.DATE, 1);
        }

        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), mNotificationReceiverPendingIntent);

        try {
            Dispatcher.queueMessage(new SetAlarm(hourOfDay, minute).getJSON());
        } catch(JSONException ex) { }
    }

    private void initApp() {
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        alarmTime = prefs.getString("alarm_time", null);
        notySoundUri = prefs.getString("noty_sound_uri", null);
    }

    private void initGUI() {

        /* AnalogClock */
        analogClock = (AnalogClock) findViewById(R.id.analogClock);

        /* SeekBar - Set current brightness */
        final SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                // Checking if the progress is changing due to an animation, in order to avoid flooding the bluetooth channel with unnecessary messages
                if(!sliderAnimating) {
                    Log.d(TAG, "Setting brightness @ " + i);
                    try {
                        Dispatcher.queueMessage(new SetBrightness(i).getJSON());
                    } catch(JSONException ex) { }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        /* Value animator end listener */
        final Animator.AnimatorListener animatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                sliderAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                sliderAnimating = false;
                if(seekBar.getProgress() == 0) {
                    Log.d(TAG, "Leds turned off");
                } else if(seekBar.getProgress() == seekBar.getMax()) {
                    Log.d(TAG, "Leds set at full brightness");
                }

                try {
                    Dispatcher.queueMessage(new TurnOnOff(seekBar.getProgress() > seekBar.getMax() - 1).getJSON());
                } catch(JSONException ex) { }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                // Not implemented
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                // Not implemented
            }
        };

        /* Alarm time view */
        alarmTimeView = (TextView) findViewById(R.id.alarmTime);
        alarmTimeView.setText(alarmTime == null ? "--:--" : alarmTime);
        alarmTimeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Adding alarm");
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getFragmentManager(), "timePicker");
            }
        });

        /* Power off leds */
        ImageButton powerOff = (ImageButton) findViewById(R.id.powerOff);
        powerOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Powering leds off");

                ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), 0);
                anim.setDuration(500);
                anim.addListener(animatorListener);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        seekBar.setProgress((Integer)valueAnimator.getAnimatedValue());
                    }
                });

                anim.start();
            }
        });

        /* Leds full brightness */
        ImageButton powerOn = (ImageButton) findViewById(R.id.powerOn);
        powerOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Settings leds to full brightness");

                ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), seekBar.getMax());
                anim.setDuration(500);
                anim.addListener(animatorListener);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        seekBar.setProgress((Integer)valueAnimator.getAnimatedValue());
                    }
                });

                anim.start();
            }
        });

        /* Remove alarm */
        ImageButton removeAlarm = (ImageButton) findViewById(R.id.deleteAlarm);
        removeAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Removing alarm");
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("alarm_time");
                editor.commit();

                alarmTimeView.setText("--:--");

                mAlarmManager.cancel(mNotificationReceiverPendingIntent);

                try {
                    Dispatcher.queueMessage(new SetAlarm(-1, -1).getJSON());
                } catch(JSONException ex) { }
            }
        });

        /* Connection status */
        connStatus = (TextView) findViewById(R.id.connStatus);

        /* Ringtone selection */
        View.OnClickListener selectRingTone = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sound = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                sound.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);

                if(notySoundUri != null) {
                    sound.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(notySoundUri));
                }

                startActivityForResult(sound, Constants.AR_SELECT_ALARMTONE);
            }
        };

        ImageButton alarmButtonImage = (ImageButton) findViewById(R.id.alarmTone);
        alarmButtonImage.setOnClickListener(selectRingTone);

        alarmButtonText = (TextView) findViewById(R.id.alarmRingtone);
        alarmButtonText.setOnClickListener(selectRingTone);
        alarmButtonText.setText(prefs.getString("noty_sound_name", "Default ringtone"));

    }

}
