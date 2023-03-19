package com.wildbitsfoundry.caveman;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;

public class CaveDataStream implements Iterable<CaveCell> {
	protected Queue<IndexTableEntry> _entryList;
	private String _data;
	private int _cols;
	private int _entryCount;
	private int _streamPointer;
	private int _length;
	private StreamIterator _streamIterator;
	private int _currentCell;
	private final int begin;

	public CaveDataStream(String data, int rows, int cols) {
		this._data = data;
		this._cols = cols;

		List<IndexTableEntry> entryList = getIndexTableEntries(data, rows, cols);

		for (IndexTableEntry entry : entryList) {
			this._entryCount += entry.EntryCount;
		}
		this._length = entryList.get(entryList.size() - 1).End;

		this._entryList = new LinkedList<>();
		this._entryList.addAll(entryList);
		this._streamIterator = new StreamIterator();

		this.begin = entryList.get(0).Begin;

		this._currentCell = 0;
		this._streamPointer = begin;
	}

	CaveDataStream(String data, int entryCount, int cols, int fromIndex, int toIndex) {
		this._data = data;
		this._entryCount = entryCount;
		this._cols = cols;
		this.begin = fromIndex;
		this._currentCell = 0;
		this._streamPointer = begin;
		this._length = toIndex;
		this._streamIterator = new StreamIterator();
		this._entryList = new LinkedList<IndexTableEntry>();
	}

	public int getColumnCount() {
		return this._cols;
	}

	public int getEntryCount() {
		return this._entryCount;
	}

	public int getRowCount() {
		return _entryCount / _cols;
	}

	@Override
	public StreamIterator iterator() {
		this._currentCell = 0;
		this._streamPointer = begin;
		return this._streamIterator;
	}

	@Override
	public StreamSpliterator spliterator() {
		return new StreamSpliterator(this);
	}

	public class StreamIterator implements Iterator<CaveCell> {
		@Override
		public void forEachRemaining(Consumer<? super CaveCell> action) {
			while (this.hasNext()) {
				action.accept(this.next());
			}
		}

		@Override
		public boolean hasNext() {
			return _streamPointer < _length;
		}

		@Override
		public CaveCell next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			++_currentCell;
			if (_currentCell == _entryCount) {
				CaveCell cell = CaveCell.CaveCellFactory(_data.substring(_streamPointer, _length));
				_streamPointer = _length;
				return cell;
			}

			int begin = _streamPointer;
			while (_data.charAt(_streamPointer) != ',') {
				++_streamPointer;
			}
			CaveCell cell = CaveCell.CaveCellFactory(_data.substring(begin, _streamPointer));
			++_streamPointer;
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

	public class StreamSpliterator implements Spliterator<CaveCell> {
		CaveDataStream _dataStream;
		StreamIterator _streamIterator;

		StreamSpliterator(CaveDataStream dataStream) {
			this._dataStream = dataStream;
			this._streamIterator = dataStream.iterator();
		}

		@Override
		public void forEachRemaining(Consumer<? super CaveCell> action) {
			while (this._streamIterator.hasNext()) {
				action.accept(this._streamIterator.next());
			}
		}

		@Override
		public int characteristics() {
			return SIZED | SUBSIZED | IMMUTABLE | NONNULL;
		}

		@Override
		public long estimateSize() {
			return this._dataStream._entryCount;
		}

		@Override
		public boolean tryAdvance(Consumer<? super CaveCell> action) {
			if (!this._streamIterator.hasNext()) {
				return false;
			}
			action.accept(this._streamIterator.next());
			return true;

		}

		@Override
		public Spliterator<CaveCell> trySplit() {
			if (this._dataStream._entryList.isEmpty()) {
				return null;
			}

			IndexTableEntry entry = _entryList.poll();
			return new StreamSpliterator(new CaveDataStream(this._dataStream._data, entry.EntryCount,
					this._dataStream._cols, entry.Begin, entry.End));
		}
	}

	protected static class IndexTableEntry {
		public final Integer Begin;
		public final Integer End;
		public final Integer EntryCount;

		public IndexTableEntry(Integer begin, Integer end, Integer entryCount) {
			this.Begin = begin;
			this.End = end;
			this.EntryCount = entryCount;
		}
	}

	protected static List<IndexTableEntry> getIndexTableEntries(final String dataStream, int rows, int cols) {
		List<IndexTableEntry> pairs = new ArrayList<>();
		int dataBeginIndex = dataStream.indexOf("\"data\":[") + "\"data\":[".length();
		int entryBeginIndex = dataStream.lastIndexOf("\"dataindextable\":", dataBeginIndex);

		if (entryBeginIndex != -1) {
			entryBeginIndex += +"\"dataindextable\":".length();
			String indexTable = dataStream.substring(entryBeginIndex,
					dataStream.indexOf(']', entryBeginIndex) + 1);
			int runningIndex = 0;
			while (indexTable.indexOf("entry", runningIndex) > 0) {
				int indexBegin = indexTable.indexOf("\"begin\":", runningIndex) + "\"begin\":".length();
				int indexEnd = indexTable.indexOf("\"end\":", indexBegin) + "\"end\":".length();
				runningIndex = indexTable.indexOf("\"numentries\":", indexEnd) + "\"numentries\":".length();

				int item1 = Integer.parseInt(indexTable.substring(indexBegin, indexTable.indexOf(",", indexBegin)))
						+ dataBeginIndex;
				int item2 = Integer.parseInt(indexTable.substring(indexEnd, indexTable.indexOf(",", indexEnd)))
						+ dataBeginIndex;
				int item3 = Integer.parseInt(indexTable.substring(runningIndex, indexTable.indexOf("}", runningIndex)));
				pairs.add(new IndexTableEntry(item1, item2, item3));
			}
		} else {
			int begin = dataBeginIndex;
			int end = dataStream.length() - 2;
			int numEntries = rows * cols;
			pairs.add(new IndexTableEntry(begin, end, numEntries));
		}
		return pairs;
	}
}