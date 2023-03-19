package com.wildbitsfoundry.caveman.examples;

import com.wildbitsfoundry.caveman.*;

import java.lang.Math;


public class PolynomialFit extends CaveGrunt {
	private double a;
	private double b;
	private double c;
	private double d;


	@Override
	public void setHeaders(String[] outHeaders) 
	{
		outHeaders[0] = "f(x)";
	}

	@Override
	public void getVariables(final CaveTree variables) {
		this.a = variables.find("a").getAsDouble();
		this.b = variables.find("b").getAsDouble();
		this.c = variables.find("c").getAsDouble();
		this.d = variables.find("d").getAsDouble();
		System.out.println(variables.toString());
	}

	@Override
	public void doGruntWork(final CaveRow inputRow, CaveRow outputRow) {
		CaveRow.RowIterator it = inputRow.iterator();
		double x = it.nextDouble();

		double fx = a * Math.pow(x, 3) + b * Math.pow(x, 2) + c * x + d;
		outputRow.set(0, CaveCell.newNumberCell(fx));
	}

	public static void main(String[] args) {
		Caveman billy = new Caveman(new PolynomialFit(), new WSServerDataSource(8675));
		billy.go();
	}
}