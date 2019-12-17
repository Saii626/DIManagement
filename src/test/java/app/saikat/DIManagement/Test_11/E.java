package app.saikat.DIManagement.Test_11;

import javax.inject.Inject;

public class E {

	private static int NO_OF_INSTANCES = 0;

	private F f;
	private G g;

	@Inject
	public E(F f, G g) {
		this.f = f;
		this.g = g;
		NO_OF_INSTANCES++;
	}

	public F getF() {
		return f;
	}

	public G getG() {
		return g;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}