package com.wildbitsfoundry.caveman;

public interface ICaveDataSource<I, O> 
{
	public void setCaveRunner(ICaveRunner<I, O> runner);
	public void go();
}
