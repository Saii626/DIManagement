package app.saikat.DIManagement.Test_7;

import javax.inject.Singleton;

@Singleton
public class E {

	private static int NO_OF_INSTANCES = 0;

	public E() {
		NO_OF_INSTANCES++;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}