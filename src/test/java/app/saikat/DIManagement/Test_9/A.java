package app.saikat.DIManagement.Test_9;

import javax.inject.Inject;

public class A {

	private static int NO_OF_INSTANCES = 0;

	private B b;
	private C c;

	public A(B b) {
		this.b = b;
		NO_OF_INSTANCES++;
	}

	@Inject
	public A(C c) {
		this.c = c;
		NO_OF_INSTANCES++;
	}

	public B getB() {
		return b;
	}

	public C getC() {
		return c;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}