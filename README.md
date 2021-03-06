## Command Mode

<p align="center"><img src="https://github.com/wholder/ADU218/blob/master/images/ADU218%20Screenshot-command.png"></p>

ADU218 provides a simple GUI interface that can send commands to an [OnTrak Control Systems](http://ontrak.net) ADU218 USB Relay I/O Interface.  To use, simply type a command into the text field at the bottom of the window and press enter.  Example commands are:

 *  **`SKn`** - Enable Relay n
 *  **`RKn`** - Disable Relay n
 *  **`MKnnn`** - Sets Relay register to nnn (where nnn >= 0 & <= 255)
 *  **`RPAn`** - Returns state of Port A, bit n (where n = 0 - 3)
 *  **`RPBn`** - Returns state of Port B, bit n (where n = 0 - 3)
 *  **`RPA`** - Returns state of Port A bits as value nn (0 - 0x0F)
 *  **`RPB`** - Returns state of Port B bits as value nn (0 - 0x0F)
 *  **`PI`** - Return state of Ports A & B as value nnn (where nnn >= 0 & <= 255)
 *  **`REn`** - Returns present count of event counter n (0-7)
 *  **`RCn`** - Returns present count and clears event counter n (0-7)
 *  **`DBn`** - Sets debounce time of event counters (0 = 10 ms, 1 = 1 ms (Default), 2 = 100 us)
 *  **`DB`** - Returns present debounce setting
 *  **`WDn`** - Sets Sets watchdog timeout length (1 = 1 sec, 2 = 10 secs, 3 = 1 min)
 *  **`WD`** - Returns watchdog setting

Note: use the optional "**`Serial`**" text field to enter the serial number of a specific ADU218, or leave blank if only one ADU218 is connected.

## Interactive Mode

<p align="center"><img src="https://github.com/wholder/ADU218/blob/master/images/ADU218%20Screenshot-interactive.png"></p>

Selecting the "Interactive" tab open up a simple GUI interface that shows the state of the inputs in real time (10 Hz refresh rate) and lets you open and close amy of the 8 relays by clicking on the corresponding checkbox.

## Counters Mode

<p align="center"><img src="https://github.com/wholder/ADU218/blob/master/images/ADU218%20Screenshot-counters.png"></p>

Selecting the "Counters" tab open up a display that shows the current state of the counters and allows you to individually reset them.

### Requirements
Java 8 JDK, or later must be installed in order to compile the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/ADU218/blob/master/out/artifacts/ADU218Test_jar) included in the checked in code that you can download.   On a Mac, just double click the **`ADU218Test.jar`** file and it should startOn a Mac.  However, you'll probably have to right click and select "Open" the  first time you run ADU218 due to new Mac OS X security checks.  You should also be able to run the JAR file on Windows or Linux systems, but you'll need to have a Java 8 JRE, or later installed and follow the appropriate process for each needed to run an executable JAR file.

## Credits
ADU218 uses the following Java code to perform some of its functions, or build this project:
- [hid4java](https://github.com/gary-rowe/hid4java) provides HID-based communication with the ADU218
- [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
