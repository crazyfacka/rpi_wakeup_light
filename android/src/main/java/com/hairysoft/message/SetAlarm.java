package com.hairysoft.message;

import java.util.Calendar;

/**
 * Message to set the next alarm
 */
public class SetAlarm extends BaseMessage {
    public int hour;
    public int minute;
    public SetAlarm() { }
    public SetAlarm(int hour, int minute) {
        // Remove 30 min to the current selected time in order to ring only when the LEDs are at full brightness
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.add(Calendar.MINUTE, -30);
        this.hour = c.get(Calendar.HOUR_OF_DAY);
        this.minute = c.get(Calendar.MINUTE);
    }
}
