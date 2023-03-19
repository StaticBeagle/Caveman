package com.wildbitsfoundry.caveman;

public class CaveInStream 
{
	private String _dataStream;
	protected String _variables;
	protected int _rows;
	protected int _columns;
	protected int _outputRows;
	protected int _outputColumns;
	protected String _GUID;
	
	public CaveInStream(final String dataStream)
	{
		this._dataStream = dataStream;
		this.parseRowsAndColumns(dataStream);
		this._variables = parseVariables(dataStream);
	}
	
	private void parseRowsAndColumns(final String dataStream)
	{
		int indexBeginRows = dataStream.indexOf("\"rows\":") + "\"rows\":".length();
		int indexEndRows = dataStream.indexOf(",", indexBeginRows);
		
		this._rows = Integer.parseInt(dataStream.substring(indexBeginRows, indexEndRows));
		
		int indexBeginCols = dataStream.indexOf("\"columns\":", indexEndRows) + "\"columns\":".length();
		int indexEndCols = dataStream.indexOf("},", indexBeginCols);

		this._columns = Integer.parseInt(dataStream.substring(indexBeginCols, indexEndCols));
		
		// get output dimensions
		indexBeginRows = dataStream.indexOf("\"rows\":", indexEndCols) + "\"rows\":".length();
		indexEndRows = dataStream.indexOf(",", indexBeginRows);
		
		this._outputRows = Integer.parseInt(dataStream.substring(indexBeginRows, indexEndRows));
		
		indexBeginCols = dataStream.indexOf("\"columns\":", indexEndRows) + "\"columns\":".length();
		indexEndCols = dataStream.indexOf("},", indexBeginCols);
		
		int indexBeginGUID = dataStream.indexOf("\"GUID\":", indexEndCols) + "\"GUID\":".length();
		int indexEndGUID = dataStream.indexOf(",", indexBeginGUID);

		this._outputColumns = Integer.parseInt(dataStream.substring(indexBeginCols, indexEndCols));
		this._GUID = dataStream.substring(indexBeginGUID, indexEndGUID);
	}
	
	private String parseVariables(final String dataStream)
	{
		int indexBeginVariables = dataStream.indexOf("\"variables\":") + "\"variables\":".length();
		int dataBeginIndex = this._dataStream.indexOf(",\"data\":", indexBeginVariables);
		int entryBeginIndex = this._dataStream.lastIndexOf(",\"dataindextable\":", dataBeginIndex);
		int indexEndVariables = entryBeginIndex == -1 ? dataBeginIndex : entryBeginIndex;
		return dataStream.substring(indexBeginVariables, indexEndVariables);
	}

	public String getRawDataStream() {
		return this._dataStream;
	}
	
	public int getRows()
	{
		return this._rows;
	}
	
	public int getColumns()
	{
		return this._columns;
	}
	
	public int getOutputRows()
	{
		return this._outputRows;
	}
	
	public int getOutputColumns()
	{
		return this._outputColumns;
	}
	
	public String getVariables()
	{
		return this._variables;
	}
	
	public String getGUID() {
		return this._GUID;
	}
	
	public CaveDataStream getDataStream()
	{
		return new CaveDataStream(this._dataStream, this._rows, this._columns);
	}
}
