package com.wildbitsfoundry.caveman;

import java.util.concurrent.ExecutionException;

public class Caveman {
	private final AbstractCaveGrunt _grunt;
	private final ICaveDataSource<String, String> _source;

	public Caveman(AbstractCaveGrunt grunt) {
		this(grunt, new WSServerDataSource(8675));
	}

	public Caveman(AbstractCaveGrunt grunt, ICaveDataSource<String, String> source) {
		this._grunt = grunt;
		this._source = source;
		this._source.setCaveRunner(this.createRunner());
	}

	public void go() {
		this._source.go();
	}

	public ICaveRunner<String, String> createRunner() {
		return serverMessage -> {
				CaveInStream cs = new CaveInStream(serverMessage);
				CaveTree tree = new CaveTree(cs.getVariables());
				return doWork(cs, tree);
		};
	}

	public String doWork(CaveInStream inStream, CaveTree variables) throws ExecutionException, InterruptedException {
		return _grunt.doGruntWork(inStream, variables);
	}
}