
package com.manicken;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class MidiHelper {

    public int selectedDeviceIndex = -1;
    public MidiDevice device;
    public Receiver rcvr;

    public MidiHelper()
    {

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

    public String[] GetDeviceList()
    {
        MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        String[] deviceNames = new String[devices.length];
        for (int i = 0; i < devices.length; i++)
        {
            deviceNames[i] = devices[i].getName() + " " + devices[i].getDescription();
        }
        return deviceNames;
    }

    public boolean OpenDevice()
    {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
       //for (int i = 0; i < infos.length; i++) {
            try {
                device = MidiSystem.getMidiDevice(infos[selectedDeviceIndex]);

                //does the device have any transmitters?
                //if it does, add it to the device list
                System.out.println(infos[selectedDeviceIndex]);

                //get all transmitters
                List<Transmitter> transmitters = device.getTransmitters();
                //and for each transmitter

                for(int j = 0; j<transmitters.size();j++) {
                    //create a new receiver
                    transmitters.get(j).setReceiver(
                        //using my own MidiInputReceiver
                        new MidiInputReceiver(device.getDeviceInfo().toString())
                    );
                }
                try{
                Transmitter trans = device.getTransmitter();
                trans.setReceiver(new MidiInputReceiver(device.getDeviceInfo().toString()));
                System.out.println("midi in");
                } catch (Exception e) {}
                
                try {
                    rcvr = device.getReceiver();
                    System.out.println("midi out");
                } catch (Exception e) {}

                //open device
                device.open();

                
                //if code gets this far without throwing an exception
                //print a success message
                System.out.println(device.getDeviceInfo()+" Was Opened");

                return true;

            } catch (MidiUnavailableException e) { e.printStackTrace(); return false; }

        //}
        //return true;
    }

}

//tried to write my own class. I thought the send method handles an MidiEvents sent to it
class MidiInputReceiver implements Receiver {
  public String name;
  public MidiInputReceiver(String name) {
    this.name = name;
  }
  public void send(MidiMessage msg, long timeStamp) {
    System.out.println("midi received" + byteArrayToString(msg.getMessage()));
  }
  public void close() {}

  private String byteArrayToString(byte[] array)
  {
      String dataHex = "";
      for (int i= 0; i < array.length; i++)
      {
          dataHex+= String.format("%02X ", array[i]);
      }
      return dataHex;
  } 
}
