package com.wildbitsfoundry.caveman;

import java.util.concurrent.ExecutionException;

public interface ICaveRunner<I, O>
{
	public O processServerMessage(I serverMessage) throws ExecutionException, InterruptedException;
}
