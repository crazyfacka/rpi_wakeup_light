Raspberry Pi - Morning wake up light
================

This little project purpose was to create something that resembles the behaviour of the Philips Wake-up Light, using a LED strip and a Raspberry Pi with Bluetooth communication.

As it is somewhat obvious, I do not accept any liability for any loss nor for burning down your house from the use of this information. Use it (and improve it, please!) at your own risk.

Preparing the Raspberry Pi
================

For the sake of this tutorial, I'm assuming all software is installed into the directory **fading_led** of the **pi** homedir. So, something like this:

```
/home/pi/fading_led
```

For this I've used a Rapsberry Pi A+ and a USB bluetooth pen carrying one of the known to work chips (more info @ http://elinux.org/RPi_USB_Bluetooth_adapters).

The steps to get the Raspberry Pi up and running are more or less the following:

1. Download and install Raspbian
	- You can get it @ http://downloads.raspberrypi.org/raspbian_latest.torrent
	- If like me you're using Linux or Mac OS, you can use **dd** to install Raspbian into an SD Card (more info @ http://www.raspberrypi.org/documentation/installation/installing-images/README.md)

2. Install pi-blaster so that you can control the LED strip intensity (basically this gives great PWM support on the GPIO pins of the Raspberry Pi)
	- Visit their GitHub and clone their repository @ https://github.com/sarfata/pi-blaster
	- Follow the instruction on their page, but in a nutshell is something like:

  ``` bash
  $ sudo apt-get install autoconf
  $ ./autogen.sh
  $ ./configure
  $ make
  $ sudo make install
  ```

3. Install bluetooth support on the Raspberry Pi
	- Execute the following commands as root

  ``` bash
  $ apt-get install bluetooth bluez-utils
  $ /etc/init.d/bluetooth status # to check if running
  $ hcitool scan # scan for devices, again, for debugging purposes
  ```
	
  - Add the following lines to the end of **/etc/rc.local**	
  
  ``` bash
  hciconfig hci0 name 'Waky Waky' # This is the device name
  hciconfig hci0 piscan # This is to turn the device discoverable
  bluetooth-agent 4321 > /dev/null 2>&1 & # This is the PIN code to use when pairing
  ```

4. Copy the files **main.py** and **wake_up.py** to the directory **/home/pi/fading_led**

5. Add this line to the end of the file **/etc/rc.local**

  ```bash
  /usr/bin/python /home/pi/fading_led/main.py > /dev/null 2>&1 &
  ```

Well now, you're all set to start using the software part of this project.

Hardware and soldering shenanigans
================

For this you need to get to your nearest hardware store and get this stuff (or similar):

1. 12v 2A DC Power supply (my LED strip was 12v)
2. 5v voltage regulator
	- The Raspberry Pi runs on 5v, so we need to get those 12v down a notch.
	- I got [L7805CV](http://uk.farnell.com/stmicroelectronics/l7805cv/ic-v-reg-5-0v-7805-to-220-3/dp/9756078).
3. 500mA fuse
	- So that you don't accidentally fry your voltage regulator.
4. 220µF capacitor
	- To prevent 5v floating, which could restart your Raspberry Pi. 
	- I got [EEUFR1C221](http://uk.farnell.com/panasonic-electronic-components/eeufr1c221/cap-alu-elec-220uf-16v-rad/dp/1907229).
5. LED strip
	- Use warm white for better effect.
	- Pay attention to the length of the strip, and its power requirements. I used 3 meters, and the 2A power supply was more than enough.
6. N-Channel MOSFET
	- Since the Raspberry Pi can't handle the LED strip directly, we'll be using this to *turn them on and off*.
	- I got [IRLZ34NPBF](http://uk.farnell.com/international-rectifier/irlz34npbf/mosfet-n-55v-27a-to-220/dp/8651396).
7. A switch
	- Choose the one that fits your needs best.
8. A regular red LED
	- I used this for debugging purposes. I turned it on as soon as the Raspberry Pi was ready.
9. A 200Ω resistor
	- The 3.3v from the Raspberry Pi GPIO is too much for the red LED.
	
So, this is it. The was this is wired can be seen in the file [sketch.fzz](https://github.com/crazyfacka/rpi_wakeup_light/blob/master/sketch.fzz). You need the app Fritzing (http://fritzing.org/home/) to open it.

Bear in mind that I am by no means an electrical engineer, and all I know I learned from the interwebs, so some schematics mistakes could be expected :)

Run
================

Install the APK into your android device. Now simply feed power to everything, wait for it to boot up, and open the app.

As soon as it finds the device, it should begin the pairing process and subsequent connection. I guess the rest of it is pretty straightforward.

ToDo
================

Yup, there's still more stuff that I would like to do. Is a project ever completed? :)

- Perform communication from the Raspberry Pi to the Android device (ex.: update LED state from button switches)
- Make the analog clock in the Android application represent the actual alarm time (source: http://stackoverflow.com/questions/16786441/set-time-with-custom-android-analogclock-from-its-original-class)
- General improvements on the Android application
- General improvements on the Raspberry Pi application(s)

Acknowledgments
================

The cat image used in the Alarm Activity was *provided* by Denis Sazhin @ https://www.iconfinder.com/iconka. The video loop used in the Alarm Activity was retrieved from a music video by Slightly Left of Centre @ https://www.youtube.com/watch?v=Wga5A6R9BJg. The colour palette used in the application it's from the user sugar and can be found @ http://www.colourlovers.com/palette/629637/(%E2%97%95%E3%80%9D%E2%97%95).
