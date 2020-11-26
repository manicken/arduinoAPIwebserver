package com.manicken;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class MyWebSocketServer extends WebSocketServer {

	public MyWebSocketServer(int port) throws UnknownHostException {
	  super(new InetSocketAddress(port));
	}
  
	public MyWebSocketServer(InetSocketAddress address) {
	  super(address);
	}
  
	public MyWebSocketServer(int port, Draft_6455 draft) {
	  super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
	}

	public void SendWithHtmlStyle(String fgColorHex, String bgColorHex, String text)
	{
		try
		{
			text = text.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>");

			broadcast("<span style=\"color:"+fgColorHex+";background-color:"+bgColorHex+";\">" + text + "</span>");
		}
		catch (Exception ex) { /*ignore*/ }
	}
  
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
	  conn.send("Welcome to the WebSocketServer!"); //This method sends a message to the new client
	  broadcast("new connection: " + handshake
		  .getResourceDescriptor()); //This method sends a message to all clients connected
	  System.out.println(
		  conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected!");
	}
  
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
	  broadcast(conn + " disconnected!");
	  System.out.println(conn + " disconnected!");
	}
  
	@Override
	public void onMessage(WebSocket conn, String message) {
	  broadcast(message);
	  System.out.println(conn + ": " + message);
	}
  
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
	  broadcast(message.array());
	  System.out.println(conn + ": " + message);
	}
  
	@Override
	public void onError(WebSocket conn, Exception ex) {
	  ex.printStackTrace();
	  if (conn != null) {
		// some errors like port binding failed may not be assignable to a specific websocket
	  }
	}
  
	@Override
	public void onStart() {
	  System.out.println("Websocket Server started!");
	  setConnectionLostTimeout(0);
	  setConnectionLostTimeout(100);
	}
  
}