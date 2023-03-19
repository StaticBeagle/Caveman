package com.wildbitsfoundry.caveman;

interface ICaveGruntBehavior<T, R> {
	public abstract void doGruntWork(final T input, final R output);
}
