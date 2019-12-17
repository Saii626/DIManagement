package app.saikat.DIManagement.Test_11;

import javax.inject.Inject;

public class G {

	private static int NO_OF_INSTANCES = 0;

	@Inject
	public G() {
		NO_OF_INSTANCES++;
	}

	public static int getNoOfInstances() {
		return NO_OF_INSTANCES;
	}
}