import os
import os.path
import sys
import math
import time

# RPI Pin for controlling the LEDs
PIN = 17
# Precision control variable
PRE = 100
# Sleep duration so that the waking up process takes about 30 min to complete
SLEEP = 3.9

# If there is a waking up process ongoing, don't run this one (This shouldn't happen though)
if os.path.isfile("/home/pi/fading_led/waking_up"):
    sys.exit()

os.system('touch /home/pi/fading_led/waking_up')

os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 0))
for a in range(0,int(math.log(100)*PRE)):
    # If file was deleted, it means that this process is to be suspended
    if not os.path.isfile("/home/pi/fading_led/waking_up"):
        sys.exit()
        
	b = a / float(PRE)
	b = math.exp(b)
	print "Value %.2f" % (b / 100)
	os.system('echo "%d=%.2f" > /dev/pi-blaster' % (PIN, b / 100))
	time.sleep(SLEEP)

# Remove file as waking up has finished
try:
    os.remove('/home/pi/fading_led/waking_up')
except:
    pass

# Set LEDs at full brightness and clear current crontab
os.system('echo "%d=%f" > /dev/pi-blaster' % (PIN, 1))
os.system('crontab -r')
