package com.manicken;

import com.manicken.MyWebSocketServer;
import com.manicken.MidiHelper;
import com.manicken.Helper;

public class MidiWebSocketBridge {
    public MidiHelper midi;
    public MyWebSocketServer mbwss; // MidiBridgeWebSocketServer

    public MidiWebSocketBridge()
    {
        midi = new MidiHelper((String message) -> {	mbwss.broadcast("midiSend(" + message + ")<br>"); });
    }

    public void startBridge(int webSocketPort)
    {
		try {
			if (mbwss != null) mbwss.stop(0);
		} catch (Exception e) { System.err.println("cannot stop prev Midi Bridge websocket server!!!"); e.printStackTrace();}
		try {
			mbwss = new MyWebSocketServer(webSocketPort, (String message) -> DecodeRawMessage(message));
            mbwss.start();
            System.out.println("Midi Bridge WebSocket Server started at port:" + webSocketPort);
		} catch (Exception e) { System.err.println("cannot start Midi Bridge websocket server!!!"); e.printStackTrace(); }
    }

    public boolean stopBridge()
    {
        midi.CloseDevices();
		// stop Midi Bridge WebSocketServer
		try {
			if (mbwss != null){ mbwss.stop(0);
            System.out.println("Midi Bridge WebSocket Server was stopped!");}
            return true;
        } catch (Exception e) { System.err.println("cannot stop prev Midi Bridge WebSocket websocket server!!!"); e.printStackTrace();}
        return false;
    }

    private void DecodeRawMessage(String message)
	{
		message = message.toLowerCase();
		if (message.startsWith("midisend")) // most common check first
			DecodeMidiMessage(message);			
		else if (message.equals("midigetdevices"))
            GetMidiDevices();
		else if (message.startsWith("midisetdevice"))
			SetMidiDevice(message.substring("midisetdevice".length()));
		else
			System.out.println("Midi Bridge data unknown command: " + message);
    }
    
	private void DecodeMidiMessage(String message)
	{
		message = Helper.GetSubStringBetween(message, "(", ")");
		if (message == null)
		{
			mbwss.broadcast("err. midi send missing (first or last) parantesis");
			return; 
		}
		System.out.println("midisend " + message);
		String[] params = message.split(",");
		if (params.length != 3) {mbwss.broadcast("err. midi send params.length != 3"); return;}
		int[] intParams = new int[params.length];
		for (int i = 0; i < 3; i++)
		{
			params[i] = params[i].trim();
			if (params[i].startsWith("0x"))
			{
				params[i] = params[i].substring(2);
				intParams[i] = Integer.parseInt(params[i], 16);
			}
			else
			{
				intParams[i] = Integer.parseInt(params[i]);
			}
			//System.out.println("midisend byte" + i + ":" + intParams[i]);
		}
		midi.Send(intParams);
	}
	public void GetMidiDevices()
	{
		String[] inDev = midi.GetInDeviceList();
		String[] outDev = midi.GetOutDeviceList();

		StringBuilder sb = new StringBuilder();
		sb.append("midiDevicesIn(");
		for (int i = 0; i < inDev.length; i++)
		{
			sb.append(inDev[i]);
			if (i < inDev.length -1)
				sb.append("\t");
		}
        sb.append(")");
        //sb.append("["+midi.selectedInDeviceIndex+"]");
        sb.append("\n");
		mbwss.broadcast(sb.toString());
		
		sb = new StringBuilder();
		sb.append("midiDevicesOut(");
		for (int i = 0; i < outDev.length; i++)
		{
			sb.append(outDev[i]);
			if (i < outDev.length -1)
				sb.append("\t");
		}
		sb.append(")");
       // sb.append("["+midi.selectedOutDeviceIndex+"]");
        sb.append("\n");
		mbwss.broadcast(sb.toString());
	}
	private void SetMidiDevice(String message)
	{
		String param = Helper.GetSubStringBetween(message, "(", ")");
		if (param == null)
		{
			mbwss.broadcast("err. midi set device missing (first or last) parantesis");
			return; 
		}
		int index = 0;
		try {index = Integer.parseInt(param);} catch (Exception ex) {mbwss.broadcast("err. midi set device index is not a integer"); return;}

		if (message.startsWith("in"))
		{
			midi.selectedInDeviceIndex = index;
			if (midi.OpenInDevice())
				System.out.println("Input: " + midi.inDevice.getDeviceInfo()+" Was Opened");
		}
		else if (message.startsWith("out"))
		{
			midi.selectedOutDeviceIndex = index;
			if (midi.OpenOutDevice())
				System.out.println("Output: " + midi.outDevice.getDeviceInfo()+" Was Opened");
		}
		else
		{
			mbwss.broadcast("err. midi set device - missing 'in' or 'out'");
			return;
		}
	}
}
