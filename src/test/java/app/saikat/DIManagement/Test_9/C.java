package app.saikat.DIManagement.Test_9;

import javax.inject.Singleton;

@Singleton
public class C {

	private static int NO_OF_INSTANCES = 0;
	
	public C() {
		NO_OF_INSTANCES++;
	}
	
	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}

}