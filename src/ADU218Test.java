import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import javax.swing.*;
import java.awt.*;

/**
 *  Demonstrate the USB HID interface using an OnTrak ADU218 Solid-State Relay I/O Interface
 *
 *  Inputs:     Optically-isolated inputs for contact or TTL Input (also accept up to 28 VDC)
 *
 *  Outputs:    Solid-State PhotoMOS Relay outputs rated 1.0A @ 120VAC, 1.0A @ 120VDC
 *
 *  Counters:   Eight, 16-bit event counters
 *
 *  Command     Response
 *    SKn         Enable Relay n
 *    RKn         Disable Relay n
 *    MKnnn       Sets Relay register to nnn (where nnn >= 0 & <= 255)
 *
 *    RPAn        Returns state of Port A, bit n (where n = 0 - 3)
 *    RPBn        Returns state of Port B, bit n (where n = 0 - 3)
 *    RPA         Returns state of Port A bits as value nn (0 - 0x0F)
 *    RPB         Returns state of Port B bits as value nn (0 - 0x0F)
 *    PI          Return state of Ports A & B as value nnn (where nnn >= 0 & <= 255)
 *
 *    REn         Returns present count of event counter n (0-7)
 *    RCn         Returns present count and clears event counter n (0-7)
 *
 *    DBn         Sets debounce time of event counters (0 = 10 ms, 1 = 1 ms (Default), 2 = 100 us)
 *    DB          Returns present debounce setting
 *
 *    WDn         Sets Sets watchdog timeout length (1 = 1 sec, 2 = 10 secs, 3 = 1 min)
 *    WD          Returns watchdog setting
 */

public class ADU218Test extends JFrame {
  private static final Integer  VENDOR_ID = 0x0A07;   // OnTrak Control Systems
  private static final Integer  PRODUCT_ID = 0x00DA;  // ADU218 USB Relay I/O Interface
  private static final int      PACKET_LENGTH = 7;
  private JTextArea             text = new JTextArea();
  private JTextField            command, serial;

  public static void main (String[] args) {
    new ADU218Test();
  }

  private ADU218Test () {
    super("ADU218Test");
    text.setColumns(80);
    text.setRows(20);
    text.setFont(new Font("Monaco", Font.PLAIN, 12));
    text.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    text.setEditable(false);
    JScrollPane scroll = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    add(scroll, BorderLayout.CENTER);
    command = new JTextField(15);
    serial = new JTextField(15);
    JPanel bottom = new JPanel(new GridLayout(0, 2));
    JPanel sPanel = new JPanel(new FlowLayout());
    JPanel cPanel = new JPanel(new FlowLayout());
    sPanel.add(new Label("Serial:"));
    sPanel.add(serial);
    cPanel.add(new Label("Command:"));
    cPanel.add(command);
    bottom.add(sPanel);
    bottom.add(cPanel);
    command.addActionListener(ev -> {
      HidServices hidServices = HidManager.getHidServices();
      try {
        String serialNum = serial.getText().toUpperCase();
        HidDevice hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, serialNum.length() > 0 ? serialNum : null);
        if (hidDevice != null) {
          if (hidDevice.isOpen()) {
            queryPortA(hidDevice, command.getText());
            command.setText("");
          }
          hidDevice.close();
        } else {
          text.append("Unable to connect to ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : ""));
        }
      } catch (Exception ex) {
        text.append(ex.toString() + "\n");
        ex.printStackTrace();
      }
    });
    add(bottom, BorderLayout.SOUTH);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    pack();
    setVisible(true);
  }

  private void queryPortA (HidDevice hidDevice, String cmd) {
    cmd = cmd.toUpperCase();
    byte[] message = new byte[PACKET_LENGTH];
    for (int ii = 0; ii < cmd.length(); ii++) {
      message[ii] = (byte) cmd.charAt(ii);
    }
    int val = hidDevice.write(message, PACKET_LENGTH, (byte) 0x01);
    if (val >= 0) {
      text.append("Snd: " + cmd + "\n");
    } else {
      text.append(hidDevice.getLastErrorMessage() + "\n");
    }
    // Read the response packet, if any
    boolean moreData = true;
    while (moreData) {
      byte[] response = new byte[PACKET_LENGTH];
      // This method will now block for 100ms or until data is read
      val = hidDevice.read(response, 100);
      switch (val) {
        case -1:
          text.append(hidDevice.getLastErrorMessage() + "\n");
          break;
        case 0:
          moreData = false;
          break;
        default:
          int rsp = 0;
          for (int ii = 1; ii < response.length; ii++) {
            if (response[ii] != 0 && response[ii] >= '0' && response[ii] <= '9') {
              rsp *= 10;
              rsp += response[ii] - '0';
            }
          }
          text.append("Rsp: 0x" + toHex(rsp) + " - " + toBin(rsp) + "b\n");
          moreData = false;
          break;
      }
    }
  }

  private String toHex (int val) {
    StringBuilder buf = new StringBuilder(Integer.toHexString(val));
    while (buf.length() < 2) {
      buf.insert(0, '0');
    }
    return buf.toString();
  }

  private String toBin (int val) {
    StringBuilder buf = new StringBuilder(Integer.toBinaryString(val));
    while (buf.length() < 8) {
      buf.insert(0, '0');
    }
    return buf.toString();
  }
}
