package com.wildbitsfoundry.caveman;

import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.*;

abstract class AbstractCaveGrunt {
	protected final ExecutorService _threadPool;

	private static String[] _secretStrings = {
			"lmhw33Kzz1c170273-a1b8-4d95-9817-d14bf8be4804_da529f46-47ae-489a-9892-5704032f20dcnngz:<>?",
			"askj43`->4384734a-013e-4f02-98e1-8f655c1c5891^8dfcf6ae-9d28-41be-9fc5-e1376515932e]w34'gh1",
			"2893y;d13c224b374-41a6-41a7-a565-3215b71302c5_bc4fd8fc-25ba-4200-9ae1-6131763990a3'2l3y-d7",
			"12h'hy-33e2b30535-16af-419f-aeb2-d8cb87410203^482f1f7d-30ce-43fb-a401-6730994b377f';ar[w-8"
		 };
	private static int secretStringLength = _secretStrings.length;

	private static class DaemonThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	}

	private static ThreadPoolExecutor newAdjustableDaemonThreadPool(int noThreads, long timeout) {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(noThreads, noThreads, timeout, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), new DaemonThreadFactory());
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	public AbstractCaveGrunt() {
		int noThreads = Runtime.getRuntime().availableProcessors();
		long timeout = 10L * 60L; // 10 * 60 seconds = 10 minutes
		this._threadPool = newAdjustableDaemonThreadPool(noThreads, timeout);
	}

	public AbstractCaveGrunt(ExecutorService threadPool) {
		this._threadPool = threadPool;
	}

	public void setUp(String payload) {
	}

	public void setHeaders(String[] outHeaders) {
		for (int i = 0; i < outHeaders.length; ++i) {
			outHeaders[i] = _secretStrings[i % secretStringLength];
		}
	}

	private static boolean headersMatchSecretStrings(String[] headers) {
		for (int i = 0; i < headers.length; ++i) {
			if(!_secretStrings[i % secretStringLength].equals(headers[i])) {
				return false;
			}
		}
		return true;
	}

	public abstract void getVariables(final CaveTree variables);

	protected String doGruntWork(CaveInStream inStream, CaveTree variables) throws ExecutionException, InterruptedException {
		this.setUp(inStream.getRawDataStream());
		int outputColums = inStream.getOutputColumns();
		int outputRows = inStream.getOutputRows();

		String[] headers = new String[outputColums];
		this.setHeaders(headers);

		boolean hasHeaders = !headersMatchSecretStrings(headers);
		if (hasHeaders) {
			--outputRows;
		}

		// Set variables
		try {
			this.getVariables(variables);
		} catch (KeyNotFoundException ex) {
			String error = ex.getMessage().replace("key", "variable");
			error += " Variables names must start with a letter, and contain" + System.lineSeparator()
					+ "only the following characters: [0-9a-zA-Z_].";
			throw new KeyNotFoundException(error);
		}
		CaveOutStreamBuilder result = this.doAllGruntWork(inStream.getDataStream(), inStream.getRows(),
				inStream.getColumns(), outputRows, outputColums);

		result.append(!hasHeaders ? Collections.emptyList() : Arrays.asList(headers));
		result.append(inStream.getGUID());
		String response = result.toString();
		this.tearDown(response);
		return response;
	}

	abstract CaveOutStreamBuilder doAllGruntWork(Iterable<CaveCell> caveDataStream, int numInputRows, int numInputCols,
			int numOutputRows, int numOutputCols) throws ExecutionException, InterruptedException;

	public void tearDown(String response) {
	}
}
