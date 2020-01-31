package app.saikat.DIManagement.Test_12;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Breaking dependency cycles using setter injection
 */
public class Test_12 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_12", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeanOfType(A.class).getProvider().get();
		B b = manager.getBeanOfType(B.class).getProvider().get();
		C c = manager.getBeanOfType(C.class).getProvider().get();
		D d = manager.getBeanOfType(D.class).getProvider().get();
		E e = manager.getBeanOfType(E.class).getProvider().get();

		assertTrue("a.b vs b", a.getB().equals(b));
		assertTrue("a.c vs c", a.getC().equals(c));
		assertTrue("b.d vs d", b.getD().equals(d));
		assertTrue("b.e vs e", b.getE().equals(e));
		assertTrue("c.d vs d", c.getD().equals(d));
		assertTrue("d.b vs b", d.getB().equals(b));
	}
}