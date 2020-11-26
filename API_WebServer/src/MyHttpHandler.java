package com.manicken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;


import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.manicken.API_WebServer;

public class MyHttpHandler implements HttpHandler
{    
	API_WebServer api;

	public MyHttpHandler(API_WebServer api)
	{
		this.api = api;
	}
	
	@Override    
	public void handle(HttpExchange httpExchange) throws IOException {
		httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		
		String reqMethod = httpExchange.getRequestMethod();
		String htmlResponse = "";
		String requestParamValue=null; 
		
		if(reqMethod.equals("GET"))
		{
			htmlResponse = api.parseGET(queryToMap(httpExchange.getRequestURI().getQuery()));
		}
		else if(reqMethod.equals("POST"))
		{ 
			requestParamValue = handlePostRequest(httpExchange);
			if (requestParamValue.length() == 0)
			{
				System.out.println("HTTP POST don't contain any data!");
				htmlResponse = "";
			}
			else
			{
				htmlResponse = api.parsePOST(requestParamValue);
			}
		}
		else
		{
			System.out.println("unknown reqMethod:" + reqMethod);
			htmlResponse = "unknown reqMethod:" + reqMethod;
		}
		//System.out.println(requestParamValue); // debug
		handleResponse(httpExchange, htmlResponse); 
	}

	public Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<>();
		for (String param : query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			}else{
				result.put(entry[0], "");
			}
		}
		return result;
	}
	
	private String handlePostRequest(HttpExchange httpExchange) {
		if (httpExchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            try{
			httpExchange.sendResponseHeaders(200, 0);
			} catch (Exception e) {
			e.printStackTrace();
			}
			System.out.println("hi");
            return "";
        }
		InputStream input = httpExchange.getRequestBody();
        StringBuilder stringBuilder = new StringBuilder();

        new BufferedReader(new InputStreamReader(input))
                          .lines()
                          .forEach( (String s) -> stringBuilder.append(s + "\n") );

		return stringBuilder.toString();
	}

	private void handleResponse(HttpExchange httpExchange, String htmlResponse)  throws  IOException {
		OutputStream outputStream = httpExchange.getResponseBody();

		// this line is a must
		httpExchange.sendResponseHeaders(200, htmlResponse.length());
		// additional data to send back
		outputStream.write(htmlResponse.getBytes());
		outputStream.flush();
		outputStream.close();
	}
}
