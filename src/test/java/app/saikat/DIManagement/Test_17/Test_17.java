package app.saikat.DIManagement.Test_17;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

public class Test_17 {

	// @Test
	public void test() {
		DIManager manager = DIManager.newInstance();
		manager.scan("app.saikat.DIManagement.Test_16", "app.saikat.DIManagement.Annotations",
				"app.saikat.DIManagement.Impl.BeanManagers");
	}
}