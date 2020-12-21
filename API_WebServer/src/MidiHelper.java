
package com.manicken;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
import java.util.function.Consumer;

public class MidiHelper {

    public int selectedInDeviceIndex = -1;
    public int selectedOutDeviceIndex = -1;
    public MidiDevice inDevice;
    public MidiDevice outDevice;
    public Receiver rcvr;

    public MidiDevice.Info[] devices;

    public List<MidiDevice.Info> inDevices;
    public List<MidiDevice.Info> outDevices;
    public List<String> inDeviceNames;
    public List<String> outDeviceNames;

    Consumer<String> onMessageHandler;

    public MidiHelper(Consumer<String> _onMessageHandler)
    {
        this.onMessageHandler = _onMessageHandler;
        devices = MidiSystem.getMidiDeviceInfo();
        inDevices = new ArrayList<MidiDevice.Info>();
        outDevices = new ArrayList<MidiDevice.Info>();
    }

    public void Send(int[] params)
    {
        Send(params[0], params[1], params[2]);
    }
    public void Send(int status, int data1, int data2)
    {
        ShortMessage  sm = new ShortMessage ();
        try {
        sm.setMessage(status, data1, data2);
        rcvr.send(sm, -1);
        }
        catch (InvalidMidiDataException imde) { }
    }

    public String GetCurrentInDeviceNameDescr()
    {
        return inDeviceNames.get(selectedInDeviceIndex);
    }
    public String GetCurrentOutDeviceNameDescr()
    {
        return outDeviceNames.get(selectedOutDeviceIndex);
    }

    public String[] GetInDeviceList()
    {
        inDevices.clear();
        devices = MidiSystem.getMidiDeviceInfo();
        inDeviceNames = new ArrayList<String>();
        for (int i = 0; i < devices.length; i++)
        {
            if (!IsInDevice(devices[i])) continue;
            inDevices.add(devices[i]);
            inDeviceNames.add(devices[i].getName() + " " + devices[i].getDescription());
        }
        return inDeviceNames.toArray(new String[0]);
    }
    public String[] GetOutDeviceList()
    {
        outDevices.clear();
        devices = MidiSystem.getMidiDeviceInfo();
        outDeviceNames = new ArrayList<String>();
        for (int i = 0; i < devices.length; i++)
        {
            if (!IsOutDevice(devices[i])) continue;
            outDevices.add(devices[i]);
            outDeviceNames.add(devices[i].getName() + " " + devices[i].getDescription());
        }
        return outDeviceNames.toArray(new String[0]);
    }
    public boolean OpenInDevice(String nameDescr)
    {
        if (nameDescr.length() == 0) return false;
        
        GetInDeviceList(); // generate list
        int index = inDeviceNames.indexOf(nameDescr);
        if (index == -1) { System.out.println("Could not found in device:" + nameDescr); return false;}
        selectedInDeviceIndex = index;
        //System.out.println("trying open midi in device: " +nameDescr);
        return OpenInDevice();
    }
    public boolean OpenOutDevice(String nameDescr)
    {
        if (nameDescr.length() == 0) return false;
        
        GetOutDeviceList(); // generate list
        int index = outDeviceNames.indexOf(nameDescr);
        if (index == -1) { System.out.println("Could not found out device:" + nameDescr); return false;}
        selectedOutDeviceIndex = index;
        //System.out.println("trying open midi out device: " +nameDescr);
        return OpenOutDevice();
    }
    public boolean OpenDevices(String inDevice, String outDevice)
    {
        boolean anyOpen = false;
        if (OpenInDevice(inDevice))
        {
            anyOpen = true;
            System.out.println("Input: " + inDevice + " Was Opened");
        }
        if (OpenOutDevice(outDevice))
        {
            anyOpen = true;
            System.out.println("Output: " + outDevice + " Was Opened");
        }
        return anyOpen;
    }
    public boolean OpenDevices()
    {
        boolean anyOpen = false;
        if (OpenInDevice())
        {
            anyOpen = true;
            System.out.println("Input: " + inDevice.getDeviceInfo()+" Was Opened");
            
        }
        if (OpenOutDevice())
        {
            anyOpen = true;
            System.out.println("Output: " + outDevice.getDeviceInfo()+" Was Opened");
        }
        return anyOpen;
    }
    public boolean IsInDevice(MidiDevice.Info device)
    {
        try {
            Transmitter trans = MidiSystem.getMidiDevice(device).getTransmitter();
            return true;
        }catch (Exception e) { return false;}
    }
    public boolean IsOutDevice(MidiDevice.Info device)
    {
        try {
            Receiver rcvr = MidiSystem.getMidiDevice(device).getReceiver();
            return true;
        }catch (Exception e) { return false;}
    }
    public boolean OpenInDevice()
    {
       //for (int i = 0; i < infos.length; i++) {
        try {
            try{
                if (inDevice != null)
                inDevice.close();
            }catch (Exception e) { }
            inDevice = MidiSystem.getMidiDevice(inDevices.get(selectedInDeviceIndex));
            try{
                if (inDevice != null)
                inDevice.close();
            }catch (Exception e) { }
            try {
                Transmitter trans = inDevice.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(inDevice.getDeviceInfo().toString(), onMessageHandler));
                inDevice.open();
                return true;
            } catch (Exception e) { e.printStackTrace(); return false; }
        } catch (MidiUnavailableException e) { e.printStackTrace(); return false; }
    }
    public boolean OpenOutDevice()
    {
        try {
            try{
                if (outDevice != null)
                outDevice.close();
            }catch (Exception e) { }
            outDevice = MidiSystem.getMidiDevice(outDevices.get(selectedOutDeviceIndex));
            try{
                if (outDevice != null)
                outDevice.close();
            }catch (Exception e) { }
            try {
                rcvr = outDevice.getReceiver();
                outDevice.open();
                return true;
            } catch (Exception e) { e.printStackTrace(); return false; }
        } catch (MidiUnavailableException e) { e.printStackTrace(); return false; }
    }
}

//tried to write my own class. I thought the send method handles an MidiEvents sent to it
class MidiInputReceiver implements Receiver {
  public String name;
  Consumer<String> onMessageHandler;
  public MidiInputReceiver(String name,Consumer<String> _onMessageHandler) {
    this.onMessageHandler = _onMessageHandler;
    this.name = name;
  }
  public void send(MidiMessage msg, long timeStamp) {
    if (onMessageHandler != null)
        onMessageHandler.accept(byteArrayToString(msg.getMessage(), ","));
    else
        System.out.println("midi received(" + byteArrayToString(msg.getMessage(), ",") + ")");
  }
  public void close() {}

  private String byteArrayToString(byte[] array, String seperator)
  {
      String dataHex = "";
      for (int i= 0; i < array.length; i++)
      {
          dataHex+= String.format("%02X ", array[i]);
          if (i < array.length - 1)
            dataHex += seperator;
      }
      return dataHex;
  } 
}
