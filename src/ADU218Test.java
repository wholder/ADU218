import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.jna.HidApi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/*
 *  Demonstrate the USB HID interface using an OnTrak ADU218 Solid-State Relay I/O Interface
 *
 *  Author: Wayne Holder, 2019
 *  License: MIT (https://opensource.org/licenses/MIT)
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
  private transient Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
  private static final Integer  VENDOR_ID = 0x0A07;   // OnTrak Control Systems
  private static final Integer  PRODUCT_ID = 0x00DA;  // ADU218 USB Relay I/O Interface
  private static final int      PACKET_LENGTH = 7;
  private JTextArea             text = new JTextArea();
  private JTextField            command, serial;

  abstract private class RunPane extends JPanel {
    transient boolean         running;
    private transient Thread  thread;

   void start () {
     running = true;
     (thread = new Thread((Runnable) this)).start();
   }

   void stop () {
     running = false;
     if (thread != null) {
       try {
         thread.join(500);
       } catch (InterruptedException ex) {
         ex.printStackTrace();
       }
       thread = null;
     }
   }
  }

  private class Counter extends JPanel {
    private transient int     count = -1;
    private transient boolean doReset;
    private JTextField        counter = new JTextField();

    Counter (int num) {
      setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
      setLayout(new BorderLayout());
      JComponent comp;
      add(comp = new JLabel((num < 4 ? "PA" + num : "PB" + (num - 4)) + " Count: "), BorderLayout.WEST);
      comp.setFont(new Font("Monoco", Font.PLAIN, 22));
      add(counter, BorderLayout.CENTER);
      counter.setFont(new Font("Monoco", Font.PLAIN, 22));
      counter.setHorizontalAlignment(SwingConstants.RIGHT);
      counter.setEditable(false);
      JButton reset = new JButton("RESET");
      add(reset, BorderLayout.EAST);
      reset.addActionListener(ev -> doReset = true);
    }

    private void setCount (int count) {
      if (this.count != count) {
        counter.setText(Integer.toString(count));
        this.count = count;
        repaint();
      }
    }
  }

  private class CounterPane extends RunPane implements Runnable {
    private JTextField        errors = new JTextField();
    private Counter[]         counters = new Counter[8];

    CounterPane () {
      setLayout(new BorderLayout());
      add(errors, BorderLayout.NORTH);
      errors.setEditable(false);
      JPanel counterPane = new JPanel(new GridLayout(8, 1));
      for (int ii = 0; ii < 8; ii ++) {
        counterPane.add(counters[ii] = new Counter(ii));
      }
      add(counterPane, BorderLayout.CENTER);
    }

    public void run () {
      errors.setText("");
      HidServices hidServices = HidManager.getHidServices();
      try {
        String serialNum = serial.getText().toUpperCase();
        HidDevice hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, serialNum.length() > 0 ? serialNum : null);
        if (hidDevice != null) {
          if (hidDevice.isOpen()) {
            while (running) {
              for (int counter = 0; counter < 8; counter++) {
                boolean reset = counters[counter].doReset;
                counters[counter].doReset = false;
                byte[] pi = new byte[]{(byte) 'R', (byte) (reset ? 'C' : 'E'), (byte) (counter + '0'), 0, 0, 0, 0};
                if (hidDevice.write(pi, PACKET_LENGTH, (byte) 0x01) >= 0) {
                  byte[] response = new byte[PACKET_LENGTH];
                  // This method will now block for 20 ms or until data is read
                  if (hidDevice.read(response, 20) > 0) {
                    int rsp = parseValue(response);
                    counters[counter].setCount(rsp);
                  }
                }
              }
              Thread.sleep(100);
            }
          } else {
            errors.setText("Unable to connect to ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + "\n");
          }
          hidDevice.close();
        } else {
          errors.setText("ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + " not found\n");
        }
      } catch (InterruptedException ex) {
        errors.setText(ex.toString());
        ex.printStackTrace();
        running = false;
      } finally {
        HidApi.exit();
      }
    }
  }

  private class Indicator extends JPanel {
    private boolean   on;

    public void paint (Graphics g) {
      Dimension size = getSize();
      g.setColor(Color.white);
      g.fillRect(0, 0, size.width, size.height);
      g.setColor(on ? Color.blue : Color.white);
      g.fillRoundRect(8, 8, size.width - 16, size.height - 16, 10, 10);
      g.setColor(Color.black);
      g.drawRoundRect(8, 8, size.width - 16, size.height - 16, 10, 10);
    }

    private void setState (boolean on) {
      if (this.on != on) {
        this.on = on;
        repaint();
      }
    }
  }

  private class Input extends JPanel {
    Indicator indicator;

    Input (String name) {
      setLayout(new GridLayout(2, 1));
      setBackground(Color.white);
      setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10),
          BorderFactory.createLineBorder(Color.black, 1, true)));
      JComponent comp;
      add(comp = new JLabel(name, SwingConstants.CENTER));
      comp.setFont(new Font("helvetica", Font.BOLD, 16));
      indicator = new Indicator();
      add(indicator);
    }

    private void setState (boolean on) {
      indicator.setState(on);
    }
  }

  private class Relay extends JPanel {
    private boolean   on;

    Relay (int number) {
      setLayout(new GridLayout(2, 1));
      setBackground(Color.white);
      setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10),
          BorderFactory.createLineBorder(Color.black, 1, true)));
      JComponent comp;
      add(comp = new JLabel("K" + number, SwingConstants.CENTER));
      comp.setFont(new Font("helvetica", Font.BOLD, 18));
      JCheckBox box = new JCheckBox();
      box.setHorizontalAlignment(SwingConstants.CENTER);
      add(box);
      box.addActionListener(ev -> on = box.isSelected());
    }

    private boolean getState () {
      return on;
    }
  }

  private class InteractivePane extends RunPane implements Runnable {
    private List<Input>       inputs = new ArrayList<>();
    private List<Relay>       relays = new ArrayList<>();
    private JTextField        errors = new JTextField();

    InteractivePane () {
      setBackground(new Color(48, 89, 205));
      setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      setLayout(new GridLayout(3, 1));
      JPanel inputPanel = new JPanel(new GridLayout(1, 8));
      for (int ii = 0; ii < 8; ii++) {
        Input input;
        if (ii < 4) {
          inputPanel.add(input = new Input("PA" + ii));
        } else {
          inputPanel.add(input = new Input("PB" + (ii - 4)));
        }
        inputs.add(input);
      }
      add(inputPanel);
      JPanel device = new JPanel(new BorderLayout());
      JLabel ADU218 = new JLabel("ADU218 Solid-State Relay I/O Interface", SwingConstants.CENTER);
      ADU218.setFont(new Font("helvetica", Font.BOLD, 28));
      device.add(ADU218, BorderLayout.CENTER);
      JPanel errPanel = new JPanel(new BorderLayout());
      errPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      errPanel.add(errors, BorderLayout.CENTER);
      device.add(errPanel, BorderLayout.SOUTH);
      errors.setEditable(false);
      add(device);
      JPanel relayPanel = new JPanel(new GridLayout(1, 8));
      for (int ii = 0; ii < 8; ii++) {
        Relay relay = new Relay(ii);
        relays.add(relay);
        relayPanel.add(relay);
      }
      add(relayPanel);
    }

    public void run () {
      errors.setText("");
      HidServices hidServices = HidManager.getHidServices();
      try {
        String serialNum = serial.getText().toUpperCase();
        HidDevice hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, serialNum.length() > 0 ? serialNum : null);
        int relayState = 0;
        if (hidDevice != null) {
          if (hidDevice.isOpen()) {
            while (running) {
              // Read inputs and update state of Indicators
              byte[] pi = new byte[]{(byte) 'P', (byte) 'I', 0, 0, 0, 0, 0};
              if (hidDevice.write(pi, PACKET_LENGTH, (byte) 0x01) >= 0) {
                byte[] response = new byte[PACKET_LENGTH];
                // This method will now block for 50 ms or until data is read
                if (hidDevice.read(response, 50) > 0) {
                  int rsp = parseValue(response);
                  int mask = 1;
                  for (Input input : inputs) {
                    input.setState((rsp & mask) != 0);
                    mask <<= 1;
                  }
                }
              }
              // Write state to relays
              int tmp = 0;
              for (Relay relay : relays) {
                tmp = (tmp >> 1) | (relay.getState() ? 0x80 : 0);
              }
              if (tmp != relayState) {
                String num = toPaddedDecimal(tmp);
                byte[] mk = new byte[]{'M', 'K', (byte) num.charAt(0), (byte) num.charAt(1), (byte) num.charAt(2), 0, 0};
                hidDevice.write(mk, PACKET_LENGTH, (byte) 0x01);
                relayState = tmp;
              }
              Thread.sleep(100);
            }
          } else {
            errors.setText("Unable to connect to ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + "\n");
          }
          hidDevice.close();
        } else {
          errors.setText("ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + " not found\n");
        }
      } catch (InterruptedException ex) {
        errors.setText(ex.toString());
        ex.printStackTrace();
        running = false;
      } finally {
        HidApi.exit();
      }
    }
  }

  private class CommandPane extends JPanel implements Runnable {
    CommandPane () {
      setLayout(new BorderLayout());
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
        new Thread(this).start();
      });
      add(bottom, BorderLayout.SOUTH);
    }

    public void run () {
      HidServices hidServices = HidManager.getHidServices();
      try {
        String serialNum = serial.getText().toUpperCase();
        HidDevice hidDevice = hidServices.getHidDevice(VENDOR_ID, PRODUCT_ID, serialNum.length() > 0 ? serialNum : null);
        if (hidDevice != null) {
          if (hidDevice.isOpen()) {
            queryADU218(hidDevice, command.getText());
          } else {
            text.append("Unable to connect to ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + "\n");
          }
          hidDevice.close();
        } else {
          text.append("ADU218" + (serialNum.length() > 0 ? " - serial: " + serialNum : "") + " not found\n");
        }
        command.setText("");
      } catch (Exception ex) {
        text.append(ex.toString() + "\n");
        ex.printStackTrace();
      } finally {
        HidApi.exit();
      }
    }

    private void queryADU218 (HidDevice hidDevice, String cmd) {
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
        // This method will now block for 50 ms or until data is read
        val = hidDevice.read(response, 50);
        switch (val) {
          case -1:
            text.append(hidDevice.getLastErrorMessage() + "\n");
            break;
          case 0:
            moreData = false;
            break;
          default:
            int rsp = parseValue(response);
            text.append("Rsp: 0x" + toHex(rsp) + " - " + toBin(rsp) + "b\n");
            moreData = false;
            break;
        }
      }
    }
  }


  private ADU218Test () {
    super("ADU218Test");
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Command", new CommandPane());
    InteractivePane iPane = new InteractivePane();
    tabs.addTab("Interactive", iPane);
    CounterPane cPane = new CounterPane();
    tabs.addTab("Counters", cPane);
    tabs.addChangeListener(ev -> {
      JTabbedPane sourceTabbedPane = (JTabbedPane) ev.getSource();
      iPane.stop();
      cPane.stop();
      switch (sourceTabbedPane.getSelectedIndex()) {
        case 1:
          iPane.start();
          break;
        case 2:
          cPane.start();
          break;
      }
    });
    add(tabs);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    pack();
    setResizable(false);
    setLocation(prefs.getInt("window.x", 10), prefs.getInt("window.y", 10));
    // Track window resize/move events and save in prefs
    addComponentListener(new ComponentAdapter() {
      public void componentMoved (ComponentEvent ev)  {
        Rectangle bounds = ev.getComponent().getBounds();
        prefs.putInt("window.x", bounds.x);
        prefs.putInt("window.y", bounds.y);
      }
    });
    setVisible(true);
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

  private String toPaddedDecimal (int val) {
    StringBuilder buf = new StringBuilder(Integer.toString(val));
    while (buf.length() < 3) {
      buf.insert(0, '0');
    }
    return buf.toString();
  }

  private int parseValue (byte[] data) {
    StringBuilder buf = new StringBuilder();
    for (int ii = 1; ii < data.length; ii++) {
      char cc = (char) data[ii];
      if (cc >= '0' && cc <= '9') {
        buf.append(cc);
      } else {
        break;
      }
    }
    return Integer.parseInt(buf.toString());
  }

  public static void main (String[] args) {
    new ADU218Test();
  }
}
