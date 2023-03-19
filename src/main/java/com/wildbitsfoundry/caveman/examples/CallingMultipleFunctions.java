package com.wildbitsfoundry.caveman.examples;

import com.wildbitsfoundry.caveman.*;

public class CallingMultipleFunctions extends CaveGrunt {
	private double[] b;
	private double ab;
	private double ac;
	private double cab;
	private double aa;
	private int functionToCall;

	@Override
	public void setHeaders(String[] outHeaders) 
	{
		String[] headers = new String[] { "header1 * b0", "header2 * b1", "header3 * b2", "header4 * b3",
		"header1 * a.b", "header2 * a.c", "header3 * c.a.b", "header4 * header4 * a.a" };
		
		for(int i = 0; i < outHeaders.length; ++i) {
			outHeaders[i] = headers[i];
		}
	}

	@Override
	public void getVariables(final CaveTree variables) {
		this.b = variables.find("b").getAsDoubleArray();
		this.ab = variables.find("a").children("b").getAsDouble();
		this.ac = variables.find("a.c").getAsDouble();
		this.cab = variables.find("c.a.b").getAsDouble();
		this.aa = variables.find("a.a").getAsDouble();
		this.functionToCall = (int) variables.find("function").getAsDouble();

		// For debugging purposes. Plus it looks cool
		System.out.println(variables);
	}

	@Override
	public void doGruntWork(final CaveRow inputRow, CaveRow outputRow) {
		CaveRow.RowIterator it = inputRow.iterator();
		double h1 = it.nextDouble();
		double h2 = it.nextDouble();
		double h3 = it.nextDouble();
		double h4 = it.nextDouble();

		CaveRow buffer = null;
		switch (functionToCall) {
		case 0:
			buffer = explicitCellsFunction(h1, h2, h3, h4);
			break;
		case 1:
			buffer = rowBuilderFunction(h1, h2, h3, h4);
			break;
		default:
			buffer = factoryMethodsFunction(h1, h2, h3, h4);
			break;
		}
		outputRow.copyCells(buffer, buffer.size());
	}

	public CaveRow explicitCellsFunction(double h1, double h2, double h3, double h4) {
		// Declaring each cell explicitly
		CaveCell cell1 = new CaveCell();
		cell1.setCellType(CaveCellType.NUMBER);
		cell1.setValue(h1 * b[0]);

		CaveCell cell2 = new CaveCell();
		cell2.setCellType(CaveCellType.NUMBER);
		cell2.setValue(h2 * b[1]);

		CaveCell cell3 = new CaveCell();
		cell3.setCellType(CaveCellType.NUMBER);
		cell3.setValue(h3 * b[2]);

		CaveCell cell4 = new CaveCell();
		cell4.setCellType(CaveCellType.NUMBER);
		cell4.setValue(h4 * b[3]);

		CaveCell cell5 = new CaveCell();
		cell5.setCellType(CaveCellType.NUMBER);
		cell5.setValue(h1 * this.ab);

		CaveCell cell6 = new CaveCell();
		cell6.setCellType(CaveCellType.NUMBER);
		cell6.setValue(h2 * this.ac);

		CaveCell cell7 = new CaveCell();
		cell7.setCellType(CaveCellType.NUMBER);
		cell7.setValue(h3 * this.cab);

		CaveCell cell8 = new CaveCell();
		cell8.setCellType(CaveCellType.NUMBER);
		cell8.setValue(h4 * h4 * this.aa);

		return new CaveRow(cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8);
	}

	public CaveRow factoryMethodsFunction(double h1, double h2, double h3, double h4) {
		// Declaring each cell explicitly
		CaveCell cell1 = CaveCell.newNumberCell(h1 * b[0]);
		CaveCell cell2 = CaveCell.newNumberCell(h2 * b[1]);
		CaveCell cell3 = CaveCell.newNumberCell(h3 * b[2]);
		CaveCell cell4 = CaveCell.newNumberCell(h4 * b[3]);
		CaveCell cell5 = CaveCell.newNumberCell(h1 * this.ab);
		CaveCell cell6 = CaveCell.newNumberCell(h2 * this.ac);
		CaveCell cell7 = CaveCell.newNumberCell(h3 * this.cab);
		CaveCell cell8 = CaveCell.newNumberCell(h4 * h4 * this.aa);

		return new CaveRow(cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8);
	}

	public CaveRow rowBuilderFunction(double h1, double h2, double h3, double h4) {
		// Using a builder
		CaveRowBuilder builder = new CaveRowBuilder(8);
		builder.appendNumberCell(h1 * b[0]).appendNumberCell(h2 * b[1]).appendNumberCell(h3 * b[2])
				.appendNumberCell(h4 * b[3]).appendNumberCell(h1 * this.ab).appendNumberCell(h2 * this.ac)
				.appendNumberCell(h3 * this.cab).appendNumberCell(h4 * h4 * this.aa);
		return builder.toCaveRow();
	}

	public static void main(String[] args) {
		Caveman billy = new Caveman(new CallingMultipleFunctions(), new WSServerDataSource(8675));
		// Caveman billy = new Caveman(new CallingMultipleFunctions());
		billy.go();
	}
}