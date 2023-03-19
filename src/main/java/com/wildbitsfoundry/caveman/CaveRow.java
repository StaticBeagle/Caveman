package com.wildbitsfoundry.caveman;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CaveRow implements Iterable<CaveCell> {
	private CaveCell[] _cellData;
	private int _currentCell = 0;
	private RowIterator _rowIterator;

	public CaveRow(CaveCell... cells) {
		this._cellData = cells;
		this._rowIterator = new RowIterator();
	}

	public CaveRow(int size) {
		CaveCell[] cells = new CaveCell[size];
		Arrays.fill(cells, CaveCell.newErrorCell(CaveCellErrorType.ERR_NA));
		_cellData = cells;
		_rowIterator = new RowIterator();
	}

	public CaveCell get(int index) {
		return this._cellData[index];
	}

	public void set(int index, CaveCell cell) {
		this._cellData[index] = cell != null ? cell : CaveCell.newErrorCell(CaveCellErrorType.ERR_NA);
	}

	void resize(int newLength) {
		if (newLength == this.size()) {
			return;
		}
		int oldLength = this.size();
		_cellData = Arrays.copyOf(_cellData, newLength);
		this._currentCell = 0;

		while (oldLength < newLength) {
			_cellData[oldLength] = CaveCell.newErrorCell(CaveCellErrorType.ERR_NA);
			oldLength++;
		}
	}

	public void copyCells(Iterable<CaveCell> cells, int length) {
		Iterator<CaveCell> it = cells.iterator();
		for(int i = 0; i < length; ++i) {
			_cellData[i] = it.next();
		}
	}

	public int size() {
		return this._cellData.length;
	}

	@Override
	public RowIterator iterator() {
		this._currentCell = 0;
		return this._rowIterator;
	}

	@Override
	public String toString() {
		return Arrays.toString(_cellData);
	}

	public class RowIterator implements Iterator<CaveCell> {
		@Override
		public boolean hasNext() {
			return _currentCell < _cellData.length;
		}

		@Override
		public CaveCell next() {
			if (!this.hasNext()) {
				throw new NoSuchElementException();
			}
			CaveCell cell = _cellData[_currentCell];
			++_currentCell;
			return cell;
		}

		public double nextDouble() {
			return this.next().getDoubleValue();
		}

		public boolean nextBoolean() {
			return this.next().getBooleanValue();
		}

		public String nextString() {
			return this.next().getStringValue();
		}
	}
}