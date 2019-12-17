package app.saikat.DIManagement.Test_3;

public class E {

	private static int noOfInstances = 0;

	public E() {
		noOfInstances++;
	}

	public static int getNoOfInstances() {
		return noOfInstances;
	}

}