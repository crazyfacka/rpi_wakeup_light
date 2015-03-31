package com.hairysoft.message;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Message to sync the clock time
 */
public class ClockMessage extends BaseMessage {
    public long timestamp;
    public ClockMessage() {
        Calendar c = Calendar.getInstance();
        if(TimeZone.getDefault().inDaylightTime(c.getTime())) {
            c.add(Calendar.HOUR, 1);
        }
        this.timestamp = new Long(c.getTime().getTime() / 1000);
    }

    public ClockMessage(long timestamp) {
        this.timestamp = timestamp;
    }
}
