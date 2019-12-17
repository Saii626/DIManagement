package app.saikat.DIManagement.Test_11;

import javax.inject.Inject;

public class B {

	private static int NO_OF_INSTANCES = 0;

	private C c;

	@Inject
	public B(C c) {
		this.c = c;
		NO_OF_INSTANCES++;
	}

	public C getC() {
		return c;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}