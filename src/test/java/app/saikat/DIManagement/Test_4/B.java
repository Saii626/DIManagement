package app.saikat.DIManagement.Test_4;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Provides;

@Singleton
@Q1
public class B {

	private C c;

	public B(C c) {
		this.c = c;
	}

	public C getC() {
		return c;
	}

	@Provides
	@Q2
	public D createD() {
		return new D("Type: Q2");
	}
}