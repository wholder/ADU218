<p align="center"><img src="https://github.com/wholder/ADU218/tree/master/images/ADU218%20Screenshot.png"></p>

# ADU218
ADU218 provides a simple GUI interface that can send commands to an [OnTrak Control Systems](http://ontrak.net) ADU218 USB Relay I/O Interface.  to use, simply type a command into the text field at the bottom of the window and press enter.  Example commands are:

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

### Requirements
Java 8 JDK, or later must be installed in order to compile the code.  There is also a [**Runnable JAR file**](https://github.com/wholder/ADU208/tree/master/out/artifacts/ADU218Test_jar) included in the checked in code that you can download.   On a Mac, just double click the **`ADU218Test.jar`** file and it should startOn a Mac, simply double click the LaserCut.jar file to run it once you've downloaded it, although you'll probably have to right click and select "Open" the  first time you run LaserCut due to new Mac OS X security checks.  You should also be able to run the JAR file on Windows or Linux systems, but you'll need to have a Java 8 JRE, or later installed and follow the appropriate process for each needed to run an executable JAR file.

## Credits
LaserCut uses the following Java code to perform some of its functions, or build this project:
- [hid4java](https://github.com/gary-rowe/hid4java) provides HID-based communication with the ADU218
- [IntelliJ IDEA from JetBrains](https://www.jetbrains.com/idea/) (my favorite development environment for Java coding. Thanks JetBrains!)
