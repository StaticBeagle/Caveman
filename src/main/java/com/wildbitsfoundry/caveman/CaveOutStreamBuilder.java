package com.wildbitsfoundry.caveman;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CaveOutStreamBuilder {
	protected static class CaveOutStreamNode {
		public String name;
		public StringBuilder data;

		public CaveOutStreamNode(String name, StringBuilder data) {
			this.name = name;
			this.data = data;
		}

		public String data() {
			return this.data.toString();
		}

		public void append(CaveCell cell) {
			this.data.append(cell.toString()).append(",");
		}

		public void append(CaveRow row) {
			for (CaveCell cell : row) {
				this.append(cell);
			}
		}

		public boolean isEmpty() {
			return this.data.length() == 0;
		}
	}

	private int _rows;
	private int _cols;

	private LinkedList<CaveOutStreamNode> _stream;

	public CaveOutStreamBuilder() {
		this._stream = new LinkedList<CaveOutStreamNode>();
		this._stream.add(new CaveOutStreamNode("streamsize", new StringBuilder()));
		this._stream.add(new CaveOutStreamNode("headers", new StringBuilder()));
		this._stream.add(new CaveOutStreamNode("GUID", new StringBuilder()));
		this._stream.add(new CaveOutStreamNode("dataindextable", new StringBuilder()));
		this._stream.add(new CaveOutStreamNode("datasection", new StringBuilder()));
	}

	protected void append(int rows, int columns) {
		this._rows = rows;
		this._cols = columns;
		CaveOutStreamNode node = this._stream.stream().filter(n -> n.name.equals("streamsize")).findFirst().get();
		// Reset the rows and columns if previously inserted
		node.data.setLength(0);
		node.data.append("{\"rows\":").append(rows).append(",\"columns\":").append(columns).append(',');
	}

	protected void append(CaveCell cell) {
		// Get the last node where data is being appended
		CaveOutStreamNode node = this._stream.getLast();
		node.append(cell);
	}

	public void append(CaveRow row) {
		// Get the last node where data is being appended
		CaveOutStreamNode node = this._stream.getLast();
		node.append(row);
	}

	protected void append(List<String> headers) {
		CaveOutStreamNode node = this._stream.stream().filter(n -> n.name.equals("headers"))
				.findFirst().get();
		node.data.append("\"headers\":[");
		if (!headers.isEmpty()) {
			for (int i = 0; i < headers.size() - 1; ++i) {
				node.data.append('"').append(headers.get(i)).append("\",");
			}
			node.data.append('"').append(headers.get(headers.size() - 1)).append('"');
		}
		node.data.append("],");
	}

	public void append(CaveOutStreamBuffer stream) {
		CaveOutStreamNode node = this._stream.getLast();
		if (node.isEmpty()) {
			node.name = "datasection";
			node.data = stream._outStream;
		} else {
			// Create a new node indicating a new entry in the set
			this._stream.add(new CaveOutStreamNode("datasection", stream._outStream));
			// Start a "new" data section
			this._stream.add(new CaveOutStreamNode("datasection", new StringBuilder()));
		}
	}

	@Override
	public String toString() {
		// Get index table
		CaveOutStreamNode node = this._stream.stream().filter(n -> n.name.equals("dataindextable"))
				.collect(Collectors.toList()).get(0);
		node.data.append("\"dataindextable\":");
		node.data.append(buildDataIndexTable()).append(",\"data\":[");
		StringBuilder outputStream = new StringBuilder();
		for (CaveOutStreamNode oNode : this._stream) {
			if (!oNode.isEmpty()) {
				outputStream.append(oNode.data());
			}
		}
		outputStream.setCharAt(outputStream.length() - 1, ']');
		outputStream.append('}');
		return outputStream.toString();
	}

	private String buildDataIndexTable() {
		List<CaveOutStreamNode> entries = this._stream.stream()
				.filter(n -> n.name.equals("datasection") && !n.isEmpty()).collect(Collectors.toList());
		int dataSectionCount = entries.size();
		int segmentLength = this._rows / dataSectionCount;
		int lastSegmentLength = segmentLength + this._rows % dataSectionCount;

		// Scale by the number of columns
		segmentLength *= this._cols;
		lastSegmentLength *= this._cols;

		int i = 0;
		StringBuilder sb = new StringBuilder();

		int begin = 0;
		int end = entries.get(i).data.length() - 1; // -1 to account for the trailing comma

		sb.append("[");
		while (true) {
			sb.append("{").append("\"entry").append(i).append("\":{\"begin\":").append(begin).append(",\"end\":")
					.append(end).append(",\"numentries\":")
					.append(i < dataSectionCount - 1 ? segmentLength : lastSegmentLength).append("}},");
			++i;
			if (i == dataSectionCount) {
				break;
			}
			begin = end + 1;
			end = begin + entries.get(i).data.length() - 1;
		}
		sb.setCharAt(sb.length() - 1, ']');
		return sb.toString();
	}

	// TODO this should be append GUID. Check other append methods as well
	public void append(String guid) {
		CaveOutStreamNode node = this._stream.stream().filter(n -> n.name.equals("GUID")).findFirst().get();
		node.data.append("\"GUID\":").append(guid).append(',');
		
	}
}