package com.wildbitsfoundry.caveman;

import java.util.List;

public interface ICaveGrunt {
	public default void setUp() {
	}

	public List<String> setHeaders();

	public void getVariables(CaveTree variables);

	CaveOutStreamBuilder doAllGruntWork(Iterable<CaveCell> caveDataStream, int numInputCols, int numOutputCols)
			throws Exception;

	public default void tearDown() {
	}
}
