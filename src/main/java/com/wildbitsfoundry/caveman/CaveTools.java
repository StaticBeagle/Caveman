package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.List;

import v8dtoa.FastDtoa;

public final class CaveTools 
{
	private CaveTools() {}

	public static double ToDouble(String str)
	{
		return Double.parseDouble(str);
	}
	
	public static String ToString(double d)
	{
	    if (d != d)
	    {
	        return "NaN";
	    }
	    if (d == Double.POSITIVE_INFINITY)
	    {
	    	 return "Infinity";
	    }
	    if (d == Double.NEGATIVE_INFINITY)
	    {
	        return "-Infinity";
	    }
	    if (d == 0.0)
	    {
	    	return "0.0";
	    }
	        
	    // Grisu can't convert all numbers so in case
	    // it fails, lets fall back to Double.toString()
        String result = FastDtoa.numberToString(d);
        if (result != null) 
        {
            return result;
        }
	    return Double.toString(d);
	}
		
	public static String[] StringSplitter(String str, char delimiter)
	{
		List<String> split = new ArrayList<String>();
		int running = 0;
		do
		{
			int begin = running;
			while(running < str.length() && str.charAt(running) != delimiter)
			{
				++running;
			}
			split.add(str.substring(begin, running));
			++running;
		}while(running <= str.length());
		String[] result = new String[split.size()];
		split.toArray(result);
		return result;
	}
	
	public static List<String> StringSplitterToList(String str, char delimiter)
	{
		List<String> split = new ArrayList<>();
		int running = 0;
		do
		{
			int begin = running;
			while(running < str.length() && str.charAt(running) != delimiter)
			{
				++running;
			}
			split.add(str.substring(begin, running));
			++running;
		}while(running <= str.length());
		return split;
	}
	
	public static void main(String[] args)
	{
		long startTime = System.currentTimeMillis();
		for(int i = 0; i < 1; ++i)
		{
			//ToString(22.21352435234);
			Double.toString(22.21352435234);
		}
		long estimatedTime = System.currentTimeMillis() - startTime;
		System.out.println(estimatedTime);
	}
}