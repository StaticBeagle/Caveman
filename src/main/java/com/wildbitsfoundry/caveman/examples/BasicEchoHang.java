package com.wildbitsfoundry.caveman.examples;


import com.wildbitsfoundry.caveman.CaveGrunt;
import com.wildbitsfoundry.caveman.CaveRow;
import com.wildbitsfoundry.caveman.CaveTree;
import com.wildbitsfoundry.caveman.Caveman;

public class BasicEchoHang extends CaveGrunt
{
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
		for(int i = 0; i < 100_000; ++i) {
			System.out.println(i);
		}
		for(int i = 0; i < inputRow.size(); ++i) {
			outputRow.set(i, inputRow.get(i));
		}
	}

    public static void main(String[] args)
    {
    	new Caveman(new BasicEchoHang()).go();
    }
}