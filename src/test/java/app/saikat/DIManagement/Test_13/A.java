package app.saikat.DIManagement.Test_13;

import javax.inject.Singleton;

@Singleton
public class A {

	private B b;

	public A(B b) {
		this.b = b;
	}

	public B getB() {
		return b;
	}
}