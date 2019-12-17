package app.saikat.DIManagement.Test_11;

import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class A {

	private Provider<B> bProvider;
	private Provider<D> dProvider;

	public A(Provider<B> bProvider, Provider<D> dProvider) {
		this.bProvider = bProvider;
		this.dProvider = dProvider;
	}

	public Provider<B> getbProvider() {
		return bProvider;
	}

	public Provider<D> getdProvider() {
		return dProvider;
	}

}