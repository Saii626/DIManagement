package app.saikat.DIManagement.Test_8;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for unmanaged annotations
 */
public class Test_8 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();

		manager.initialize("app.saikat.DIManagement.Test_8", "app.saikat.Annotations.DIManagement");

		DIBean<A> bean = manager.getBeansOfType(A.class).iterator().next();


		assertTrue("No provider created", bean.getProvider() == null);
	}

}