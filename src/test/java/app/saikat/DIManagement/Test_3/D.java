package app.saikat.DIManagement.Test_3;

import javax.inject.Singleton;

public class D {

	private B b;
	private C c;

	public D(B b, C c) {
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