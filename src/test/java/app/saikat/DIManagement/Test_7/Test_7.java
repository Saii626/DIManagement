package app.saikat.DIManagement.Test_7;

import static org.junit.Assert.assertTrue;

import javax.inject.Provider;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for static provider
 */
public class Test_7 {

	
	@Test
	@SuppressWarnings("unused")
	public void test() {
		DIManager manager = DIManager.newInstance();
		manager.scan("app.saikat.DIManagement.Test_7", "app.saikat.Annotations.DIManagement", "app.saikat.DIManagement.Impl.BeanManagers");

		Provider<D> d = manager.getBeansOfType(TypeToken.of(D.class)).iterator().next().getProvider();

		assertTrue("A was not created", A.getNoOfInstances() == 0);
		assertTrue("B was not created", B.getNoOfInstances() == 0);
		assertTrue("C was not created", C.getNoOfInstances() == 0);
		assertTrue("D was not created", D.getNoOfInstances() == 0);
		assertTrue("E was not created", E.getNoOfInstances() == 0);
		assertTrue("F was not created", F.getNoOfInstances() == 0);

		D d1 = d.get();
		D d2 = d.get();
		D d3 = d.get();

		assertTrue("A was not created", A.getNoOfInstances() == 0);
		assertTrue("B was not created", B.getNoOfInstances() == 0);
		assertTrue("C was not created", C.getNoOfInstances() == 0);
		assertTrue("D has 1 instance", D.getNoOfInstances() == 1);
		assertTrue("E has 1 instance", E.getNoOfInstances() == 1);
		assertTrue("F has 1 instance", F.getNoOfInstances() == 1);
	}
}