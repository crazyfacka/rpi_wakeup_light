package com.hairysoft.message;

/**
 * Messate to turn the LEDs OFF or ON
 */
public class TurnOnOff extends BaseMessage {
    public boolean state;
    public TurnOnOff() { }
    public TurnOnOff(boolean state) {
        this.state = state;
    }
}
