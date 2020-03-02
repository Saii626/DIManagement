package app.saikat.DIManagement.Test_14;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Exceptions.BeanNotFoundException;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Testing proper functioning of constructor generator functions
 */
public class Test_14 {

	@Test
	public void test() throws BeanNotFoundException {

		DIManager manager = DIManager.newInstance();
		manager.scan("app.saikat.DIManagement.Test_14", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeanOfType(TypeToken.of(A.class)).getProvider().get();
		D d = manager.getBeanOfType(TypeToken.of(D.class)).getProvider().get();
		C c = d.generateC();

		assertNotNull(a);
		assertNotNull(d);

		assertTrue("c.a vs a", c.getA().equals(a));
		assertTrue("c.b.str vs Hello world", c.getB().getStr().equals("Hello world"));

	}
}