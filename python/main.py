import os
import os.path
import json
import math
import time
import shlex
import signal
import threading
import subprocess
import RPi.GPIO as GPIO
from bluetooth import *


#############
# FUNCTIONS #
#############

CUR_HOUR = 0
CUR_MINUTE = 0
CUR_BRIGHT = 0

# RPI Pin for controlling the LEDs
PIN = 17
# RPI Pin for listening for the button input
PIN_BUTTON = 22

# Speed control variables
PRE = 100
FAST_PRE = 10
        

# Setting clock time (sync with device due to lack of RTC on RPI)
def set_clock(data):
    print "Setting clock to UNIX timestamp of %d" % data["timestamp"]
    os.system("date +%s -s @" + str(data["timestamp"]))


# Setting LEDs brightness
def set_brightness(data):
    global CUR_BRIGHT

    # Math to set to full bright or no bright given borderline input
    b = data["brightness"] / float(100)
    b = math.exp(b)
    if b > 99:
        b = 100
    elif b <= 1.1:
        b = 0
        
    print "Setting brightness @ %.2f" % (b / 100)
    os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))
    CUR_BRIGHT = data["brightness"]
    

# Does a fast demo of the sunrise    
def demo_sunrise_threaded(data):
    global CUR_BRIGHT
    print "Demoing sunrinse"
    os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 0))
    for a in range(0,int(math.log(100)*PRE)):
    	b = a / float(PRE)
    	b = math.exp(b)
    	os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))

    os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 1))
    CUR_BRIGHT = 460
    

# Starts the thread that does the quick sunrise (so that the RPI continues listening for incoming messages)
def demo_sunrise(data):
    t = threading.Thread(target=demo_sunrise_threaded, args=(data,))
    t.daemon = True
    t.start()
    

# Sets current alarm time in the system crontab, erasing previous ones
def set_alarm(data):
    global CUR_HOUR, CUR_MINUTE
    if data["hour"] == CUR_HOUR and data["minute"] == CUR_MINUTE:
        return
    
    CUR_HOUR = data["hour"]
    CUR_MINUTE = data["minute"]
    print "Setting alarm for %d:%d" % (data["hour"], data["minute"])
    os.system('echo "%d %d * * * /usr/bin/python /home/pi/fading_led/wake_up.py" | crontab -' % (data["minute"], data["hour"]))
    

# Alternates the LEDs between off and full brightness, starting from their current state.
# If the LEDs are ON (even with very low brightness), they're first turned completely off
def turn_on_off_threaded(data):
    global CUR_BRIGHT
    temp_bright = CUR_BRIGHT / 10
    if data["state"]:
        print "Turning leds ON"
        for a in range(temp_bright, int(math.log(100)*FAST_PRE)):
            b = a / float(FAST_PRE)
            b = math.exp(b)
            os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))

        os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 1))
        CUR_BRIGHT = 460
    else:
        print "Turning leds OFF"
        for a in range(temp_bright, 0, -1):
            b = a / float(FAST_PRE)
            b = math.exp(b)
            os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))

        os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 0))
        CUR_BRIGHT = 0
        
        
# Starts the thread that does the turning ON & OFF (so that the RPI continues listening for incoming messages)
def turn_on_off(data):
    t = threading.Thread(target=turn_on_off_threaded, args=(data,))
    t.daemon = True
    t.start()
        

# Dictionary to map function messages to their respective functions
options = { "clockmessage": set_clock,
            "setbrightness": set_brightness,
            "demosunrise": demo_sunrise,
            "setalarm": set_alarm,
            "turnonoff": turn_on_off }
            
##################
# BUTTON HANDLER #
##################

# Alternates the LEDs ON or OFF everytime the button is pressed
# If there is a waking up process ongoing, it schedules it for termination and places the LEDs on 50% brightness
def reset_and_change_state():
    global CUR_BRIGHT
    print "Button pressed"
    if os.path.isfile("/home/pi/fading_led/waking_up"):
        print "There is waking up in progress"
        try:
            os.remove('/home/pi/fading_led/waking_up')
        except:
            pass
        
        for a in range(0, int(math.log(100)*FAST_PRE) / 2):
            b = a / float(FAST_PRE)
            b = math.exp(b)
            os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))

        CUR_BRIGHT = 230
    else:
        print "Just switching state"
        data = dict()
        if CUR_BRIGHT > 0:
            data["state"] = False
        else:
            data["state"] = True
        turn_on_off(data)
    

# Function that is listening to button input
def handle_button():
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(PIN_BUTTON, GPIO.IN, pull_up_down=GPIO.PUD_UP)
    
    while True:
        input = GPIO.input(PIN_BUTTON)
        if input == False:
            reset_and_change_state()
            time.sleep(1.5)
        time.sleep(0.1)
            

##########
# BRAINZ #
##########

left_over = ""

# Calls the function defined for a given message type, sending the JSON message as argument
def process_message(msg):
    # print "Processing [%s]" % msg
    data = json.loads(msg)
    options[data["type"]](data)
    

# Parses the incoming messages, as they don't necessarily come isolated in the channel
# NOTE: This method is not prepared to handle objects within JSON messages
def parse_message(msg):        
    global left_over
    
    msg = left_over + msg
    to_jsonify = ""
    state = 0
    
    if msg[0] != "{":
        left_over = ""
        return
    
    for c in msg:
        if state == 1:
            to_jsonify += c
            if c == "}":
                process_message(to_jsonify)
                to_jsonify = ""
                state = 0
            
        elif state == 0 and c == "{":
            state = 1
            to_jsonify += c
            
    left_over = to_jsonify


# Main socket loop
# Listens for connections, reads messages and sends them for processing
# Based on the RPI examples: rfcomm-server.py
def sock_loop():    
    server_sock=BluetoothSocket( RFCOMM )
    server_sock.bind(("",PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "62d1e4b0-848f-11e4-b4a9-0800200c9a66"

    advertise_service( server_sock, "WakyWaky",
                       service_id = uuid,
                       service_classes = [ uuid, SERIAL_PORT_CLASS ],
                       profiles = [ SERIAL_PORT_PROFILE ], 
    #                   protocols = [ OBEX_UUID ] 
                        )

    print "Waiting for connection on RFCOMM channel %d" % port

    client_sock, client_info = server_sock.accept()

    print "Accepted connection from ", client_info

    try:
        while True:
            data = client_sock.recv(1024)
            if len(data) == 0: break
            print "received [%s]" % data
            parse_message(data)
    except IOError:
        pass

    print "disconnected"

    client_sock.close()
    server_sock.close()

    print "all done"
    
    
########
# MAIN #
########

# Launches the button listener thread
btn = threading.Thread(target=handle_button)
btn.daemon = True
btn.start()

# Yup, never give up on listening for bluetooth connections
while True:
    sock_loop()
