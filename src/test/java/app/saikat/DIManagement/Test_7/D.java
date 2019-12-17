package app.saikat.DIManagement.Test_7;

public class D {

private static int NO_OF_INSTANCES = 0;

	public D() {
		NO_OF_INSTANCES++;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}

}