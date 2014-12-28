package com.hairysoft.message;

/**
 * Message to set the current LEDs brightness
 */
public class SetBrightness extends BaseMessage {
    public int brightness;
    public SetBrightness(int brightness) {
        this.brightness = brightness;
    }
}
