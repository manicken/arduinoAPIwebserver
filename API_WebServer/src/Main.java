
package com.manicken;

import javax.lang.model.util.ElementScanner6;

import com.manicken.MidiWebSocketBridge;

public class Main {

	static MidiWebSocketBridge mwsb;

    public static void main(String[] args)
	{
		int port = 3001;
		mwsb = new MidiWebSocketBridge();
		if (args.length > 0)
		{
			if (args[0].equals("listdevices"))
			{
				PrintMidiDevices();
				return;
			}
			else if (args[0].equals("port") && args.length > 1)
				port = Integer.parseInt(args[1]);
			else
				System.out.println("unknown parameter:" + args[0]);
		}
		else
			System.out.println("no arguments");
		
		mwsb.startBridge(port);

		System.out.println("bridge started");

		while (true)
		{
			try { Thread.sleep(1000);} catch (Exception e) {}
		}
	}

	public static void PrintMidiDevices()
	{
		String[] inDev = mwsb.midi.GetInDeviceList();
		String[] outDev = mwsb.midi.GetOutDeviceList();
		System.out.println("\nInput Devices:");
		System.out.println("----------------");
		for (int i = 0; i < inDev.length; i++)
		{
			System.out.println("["+i+"] " + inDev[i]);
		}
		System.out.println("\nOutput Devices:");
		System.out.println("----------------");
		for (int i = 0; i < outDev.length; i++)
		{
			System.out.println("["+i+"] " + outDev[i]);
		}
	}
}
