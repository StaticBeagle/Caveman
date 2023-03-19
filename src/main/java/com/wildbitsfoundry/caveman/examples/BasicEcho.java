package com.wildbitsfoundry.caveman.examples;

import com.wildbitsfoundry.caveman.CaveGrunt;
import com.wildbitsfoundry.caveman.CaveRow;
import com.wildbitsfoundry.caveman.CaveTree;
import com.wildbitsfoundry.caveman.Caveman;

public class BasicEcho extends CaveGrunt
{
	
	@Override
	public void setUp(String payload) {
		System.out.println("Begin processing");
		System.out.println(payload);
	}
	
	@Override
	public void setHeaders(String[] outHeaders) 
	{
		for(int i = 0; i < outHeaders.length; ++i) {
			outHeaders[i] = "header" + (i + 1);
		}
	}

	@Override
	public void getVariables(final CaveTree variables)
	{
		System.out.println(variables.toString());
	}

	@Override
	public void doGruntWork(final CaveRow inputRow, CaveRow outputRow)
	{
		for(int i = 0; i < inputRow.size(); ++i) {
			outputRow.set(i, inputRow.get(i));
		}
	}
	
	@Override
	public void tearDown(String response) {
		System.out.println(response);
		System.out.println("Done processing");
	}
	

    public static void main(String[] args)
    {
    	new Caveman(new BasicEcho()).go();
    }
}