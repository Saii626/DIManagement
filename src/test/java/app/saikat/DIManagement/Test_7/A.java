package app.saikat.DIManagement.Test_7;

import javax.inject.Singleton;

@Singleton
public class A {

	private static int NO_OF_INSTANCES = 0;

	private B b;

	public A(B b) {
		this.b = b;
		NO_OF_INSTANCES++;
	}

	public B getB() {
		return b;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}