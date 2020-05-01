package app.saikat.DIManagement.Test_13;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Exceptions.BeanNotFoundException;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Testing proper functioning of method generator functions
 */
public class Test_13 {

	@Test
	public void test() throws BeanNotFoundException {

		DIManager manager = DIManager.newInstance();
		manager.scan("app.saikat.DIManagement.Test_13", "app.saikat.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeanOfType(TypeToken.of(A.class)).getProvider().get();
		G g = manager.getBeanOfType(TypeToken.of(G.class)).getProvider().get();
		I i = manager.getBeanOfType(TypeToken.of(I.class)).getProvider().get();
		H h = i.createH();
		E e = h.getE();

		assertTrue("i not null", i != null);
		assertTrue("h nt null", h != null);
		assertTrue("h.a vs a", h.getA().equals(a));
		assertTrue("h.c not null", h.getC() != null);
		assertTrue("h.d is specified str", h.getD().getStr().equals("hello world"));
		assertTrue("h.e.g vs g ", e.getG().equals(g));
		assertTrue("h.e has specified str", e.getStr().equals("Hello world"));
	}
}