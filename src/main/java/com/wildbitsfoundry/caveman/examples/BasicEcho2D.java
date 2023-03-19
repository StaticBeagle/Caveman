package com.wildbitsfoundry.caveman.examples;

import com.wildbitsfoundry.caveman.*;

import java.util.List;


public class BasicEcho2D extends CaveGrunt2D
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
	public void doGruntWork(final List<CaveRow> inputRows, List<CaveRow> outputRows)
	{
        int m = inputRows.size();
        int n = inputRows.get(0).size();

        for(int i = 0; i < n; ++i) {
            CaveCell[] entries = new CaveCell[m];
            for(int j = 0; j < m; ++j) {
                entries[j] = inputRows.get(j).get(i);
            }
            outputRows.add(new CaveRow(entries));
		}
	}

    public static void main(String[] args)
    {
    	new Caveman(new BasicEcho2D()).go();
    }
}