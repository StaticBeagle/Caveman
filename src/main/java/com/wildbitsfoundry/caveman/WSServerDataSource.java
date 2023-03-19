package com.wildbitsfoundry.caveman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class WSServerDataSource implements ICaveDataSource<String, String>
{
	private ICaveRunner<String, String> _runner;
	private final int _port;
	
	public WSServerDataSource(int port)
	{
		this._port = port;
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
		if(isPortAvailable(this._port))
		{
			try 
			{
				WSServer server = new WSServer(this._port);
				server.start();
				server.setCaveRunner(this._runner);
				System.out.printf("Server started on port: %d%n", this._port);
				BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
				while (true) 
				{
					String in = sysin.readLine();
					
					if( in.equals("exit"))
					{
						server.stop();
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
				System.out.println("Server stopped");
			}
		}
		else
		{
			System.out.println("Address is already in use. Please try a different port");
		}
	}
	
	private static boolean isPortAvailable(int port) 
	{
	    try(ServerSocket s = new ServerSocket(port)) 
	    {
	        return true;
	    } 
	    catch (IOException e) 
	    {
	        return false;
	    }
	}
}
