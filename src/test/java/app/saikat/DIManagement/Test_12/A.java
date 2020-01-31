package app.saikat.DIManagement.Test_12;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class A {

	private B b;
	private C c;

	@Inject
	public A(B b, C c) {
		this.b = b;
		this.c = c;
	}

	public B getB() {
		return b;
	}

	public C getC() {
		return c;
	}

}