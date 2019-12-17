package app.saikat.DIManagement.Test_10;

import javax.inject.Inject;

public class E {

	private A a;
	private C c;

	@Inject
	public E(A a, C c) {
		this.a = a;
		this.c = c;
	}

	public C getC() {
		return c;
	}
	
	public A getA() {
		return a;
	}
}