package app.saikat.DIManagement.Test_7;

import javax.inject.Singleton;

@Singleton
public class F {

	private static int NO_OF_INSTANCES = 0;

	public F() {
		NO_OF_INSTANCES++;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}

}