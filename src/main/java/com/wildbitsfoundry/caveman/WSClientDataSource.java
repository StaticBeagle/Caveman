package com.wildbitsfoundry.caveman;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import org.java_websocket.drafts.Draft_17;

public class WSClientDataSource implements ICaveDataSource<String, String>
{
	private ICaveRunner<String, String> _runner;
	private final String _uri;
	
	public WSClientDataSource(String uri)
	{
		this._uri = uri;
	}

	@Override
	public void setCaveRunner(ICaveRunner<String, String> runner)
	{
		this._runner = runner;
	} 

	@Override
	public void go()
	{
		// shut down hook needed?

		try 
		{
			WSClient client = new WSClient(new URI(this._uri), new Draft_17()); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
			client.connect();
			client.setCaveRunner(this._runner);
			System.out.printf("Listening to port: %d%n", client.getURI().getPort());
			BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
			while (true) 
			{
				String in = sysin.readLine();
				
				if( in.equals("exit"))
				{
					client.close();
					break;
				}
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally
		{
			System.out.println("Connection closed");
		}
	}
	

}


