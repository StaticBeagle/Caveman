package com.wildbitsfoundry.caveman;

import java.net.URI;
import java.util.concurrent.ExecutionException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

/** This example demonstrates how to create a websocket connection to a server. Only the most important callbacks are overloaded. */
public class WSClient extends WebSocketClient 
{

	ICaveRunner<String, String> _runner;
	public WSClient( URI serverUri , Draft draft ) 
	{
		super( serverUri, draft );
	}

	public WSClient( URI serverURI ) 
	{
		super( serverURI );
	}

	@Override
	public void onOpen( ServerHandshake handshakedata ) 
	{
		System.out.println( "opened connection" );
		// if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
	}

	@Override
	public void onMessage(String message) 
	{
		long startTripTime = System.nanoTime();
		
		System.out.printf("%nServer: sent %d bytes%n", message.length());
		String output = null;
		try {
			output = this._runner.processServerMessage(message);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.printf("server response: %d bytes%n", output.length());
		this.send(output);
		
		long estimatedTripTime = System.nanoTime() - startTripTime;
		System.out.printf("roundtrip time: %f seconds%n", estimatedTripTime * 1e-9);
	}

	@Override
	public void onClose( int code, String reason, boolean remote ) 
	{
		// The codecodes are documented in class org.java_websocket.framing.CloseFrame
		System.out.println( "Connection closed by " + ( remote ? "remote peer" : "us" ) );
	}

	@Override
	public void onError( Exception ex ) 
	{
		ex.printStackTrace();
		// if the error is fatal then onClose will be called additionally
	}
	
	public void setCaveRunner(ICaveRunner<String, String> runner)
	{
		this._runner = runner;
	}
}