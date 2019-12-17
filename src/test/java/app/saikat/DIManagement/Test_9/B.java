package app.saikat.DIManagement.Test_9;

import javax.inject.Singleton;

@Singleton
public class B {

	private static int NO_OF_INSTANCES = 0;

	public B() {
		NO_OF_INSTANCES++;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}

}