package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.List;

public class CaveRowBuilder 
{
	private List<CaveCell> _cells;
	public CaveRowBuilder()
	{
		this._cells = new ArrayList<CaveCell>();
	}
	
	public CaveRowBuilder(int capacity)
	{
		this._cells = new ArrayList<CaveCell>(capacity);
	}
	
	public CaveRowBuilder appendNumberCell(double value)
	{
		CaveCell cell = new CaveCell();
		cell.setCellType(CaveCellType.NUMBER);
		cell.setValue(value);
		this._cells.add(cell);
		return this;
	}
	
	public CaveRowBuilder appendBooleanCell(boolean value)
	{
		CaveCell cell = new CaveCell();
		cell.setCellType(CaveCellType.BOOLEAN);
		cell.setValue(value);
		this._cells.add(cell);
		return this;
	}
	
	public CaveRowBuilder appendTextCell(String value)
	{
		CaveCell cell = new CaveCell();
		cell.setCellType(CaveCellType.TEXT);
		cell.setValue(value);
		this._cells.add(cell);
		return this;
	}
	
	public CaveRowBuilder appendErrorCell(CaveCellErrorType value)
	{
		CaveCell cell = new CaveCell();
		cell.setCellType(CaveCellType.ERROR);
		cell.setValue(value);
		this._cells.add(cell);
		return this;
	}
	
	public CaveRow toCaveRow()
	{
		CaveCell[] cells = new CaveCell[this._cells.size()];
		this._cells.toArray(cells);
		return new CaveRow(cells);
	}
}
