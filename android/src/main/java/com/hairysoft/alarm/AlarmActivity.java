package com.hairysoft.alarm;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import com.hairysoft.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.VideoView;

import com.hairysoft.cockcrow.R;
import com.hairysoft.util.Constants;

import java.io.IOException;

/**
 * Activity for the Alarm going off
 */

public class AlarmActivity extends Activity {

    private static final String TAG = "AlarmActivity";

    private VideoView vv;
    private MediaPlayer ringtone;

    private GestureDetector mGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable the activity to run over even a locked screen, and in fullscreen

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_alarm);

        // Detect fling movement on the cat in the middle

        mGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
                        Log.d(TAG, "Fling detected");
                        finish();
                        return true;
                    }
                });

        // Attach gesture detector

        ((ImageButton) findViewById(R.id.buBye)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mGestureDetector.onTouchEvent(event);
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        vv.stopPlayback();
        ringtone.stop();
        ringtone.release();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAlarm();
    }

    private void startAlarm() {

        vv = (VideoView) findViewById(R.id.videoView);
        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true); // Keep the video on continuous loop
            }
        });

        // Load the video from internal APK resources

        Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.wake_up_dance);
        vv.setVideoURI(video);
        vv.start();

        // Load Alarm sound from shared preferences

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String notySoundUri = prefs.getString("noty_sound_uri", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("alarm_time");
        editor.commit();

        try {

            // Play alarm tone in a continuous loop

            ringtone = new MediaPlayer();

            ringtone.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });

            ringtone.setDataSource(notySoundUri);
            ringtone.setLooping(true);
            ringtone.setAudioStreamType(AudioManager.STREAM_ALARM);
            ringtone.prepareAsync();

        } catch(IOException ex) { }

    }

}
