package com.hairysoft.message;

/**
 * Message to sync the clock time
 */
public class ClockMessage extends BaseMessage {
    public long timestamp;
    public ClockMessage() {
        this.timestamp = System.currentTimeMillis() / 1000L;
    }

    public ClockMessage(long timestamp) {
        this.timestamp = timestamp;
    }
}
