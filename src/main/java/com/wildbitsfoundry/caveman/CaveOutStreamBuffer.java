package com.wildbitsfoundry.caveman;

public class CaveOutStreamBuffer 
{
	
	StringBuilder _outStream;
	public CaveOutStreamBuffer()
	{
		this._outStream = new StringBuilder();
	}
	
	public CaveOutStreamBuffer(int capacity)
	{
		this._outStream = new StringBuilder(capacity);
	}
	
	protected void append(CaveCell cell)
	{
		this._outStream.append(cell.toString()).append(",");
	}
	
	public void append(CaveRow row)
	{
		for(CaveCell cell : row)
		{
			this.append(cell);
		}
	}
}