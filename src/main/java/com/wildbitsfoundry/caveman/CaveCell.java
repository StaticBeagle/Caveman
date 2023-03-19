package com.wildbitsfoundry.caveman;

public class CaveCell
{
	private double _doubleValue;
	private String _stringValue;
	private boolean _booleanValue;
	private CaveCellErrorType _errorValue;
	protected CaveCellType _cellType;
	
	public CaveCell()
	{
		this._cellType = CaveCellType.MISSING;
	}

	public static CaveCell newErrorCell(CaveCellErrorType type) {
		CaveCell cell = new CaveCell();
		cell._cellType = CaveCellType.ERROR;
		cell.setValue(type);
		return cell;
	}

	public static CaveCell newNumberCell(double number) {
		CaveCell cell = new CaveCell();
		cell._cellType = CaveCellType.NUMBER;
		cell.setValue(number);
		return cell;
	}

	public static CaveCell newStringCell(String str) {
		CaveCell cell = new CaveCell();
		cell._cellType = CaveCellType.TEXT;
		cell.setValue(str);
		return cell;
	}

	public static CaveCell newBooleanCell(boolean b) {
		CaveCell cell = new CaveCell();
		cell._cellType = CaveCellType.BOOLEAN;
		cell.setValue(b);
		return cell;
	}
	
	protected static CaveCell CaveCellFactory(String str)
	{
		CaveCell cell = new CaveCell();
		if(str.charAt(0) == '\"')
		{
			cell.setCellType(CaveCellType.TEXT);
			cell.setValue(str.substring(1, str.length() - 1));
		}
		else if(Character.isDigit(str.charAt(0)) || str.charAt(0) == '.' || str.charAt(0) == '-')
		{
			cell.setCellType(CaveCellType.NUMBER);
			cell.setValue(CaveTools.ToDouble(str));
		}
		else if(str.equalsIgnoreCase("true"))
		{
			cell.setCellType(CaveCellType.BOOLEAN);
			cell.setValue(true);
		}
		else if(str.equalsIgnoreCase("false"))
		{
			cell.setCellType(CaveCellType.BOOLEAN);
			cell.setValue(false);
		}
		// Some other types ... ?
		return cell;
	}
	
	public String getStringValue()
	{
		return this._stringValue.toString();
	}
	
	public double getDoubleValue()
	{
		return this._doubleValue;
	}
	
	public boolean getBooleanValue()
	{
		return this._booleanValue;
	}
	
	public void setCellType(CaveCellType cellType)
	{
		this._cellType = cellType;
	}
	
	public void setValue(double value)
	{
		this._doubleValue = value;
	}
	
	public void setValue(boolean value)
	{
		this._booleanValue = value;
	}
	
	public void setValue(CaveCellErrorType value)
	{
		this._errorValue = value;
	}
	
	public void setValue(String value)
	{
		this._stringValue = value;
	}

	public CaveCellType getCellType() {
		return this._cellType;
	}
	
	@Override
	public String toString()
	{
		switch(this._cellType)
		{
			case TEXT:
				return new StringBuilder("\"").append(this._stringValue).append("\"").toString();
			case BOOLEAN:
				return Boolean.toString(this._booleanValue);
			case NUMBER:
				return Double.isFinite(this._doubleValue) ? CaveTools.ToString(this._doubleValue) : "\"#NUM!\""; 
			case ERROR:
				return getErrorByName(this._errorValue);
			default:
				return "\"#N/A!\"";
		}
	}
	
	// This can be done in the enum per-se but I don't like the String
	// enum thing
	private static String getErrorByName(CaveCellErrorType errorType)
	{
		switch(errorType)
		{
			case ERR_DIV_0:
				return "\"#DIV/0!\"";
			case ERR_NA:
				return "\"#N/A!\"";
			case ERR_NAME:
				return "\"#NAME!\"";
			case ERR_NULL:
				return "\"#NULL!\"";
			case ERR_NUM:
				return "\"#NUM!\"";
			case ERR_REF:
				return "\"#REF!\"";
			case ERR_VALUE:
				return "\"#VALUE!\"";
			default:
				return "\"#N/A!\"";
		}
	}
}