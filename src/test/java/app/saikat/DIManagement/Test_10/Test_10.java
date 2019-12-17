package app.saikat.DIManagement.Test_10;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 *  Test for injection of DIManager
 */
public class Test_10 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();

		manager.initialize("app.saikat.DIManagement.Test_10", "app.saikat.Annotations.DIManagement");

		A a = manager.getBeanOfType(A.class).getProvider().get();
		B b = manager.getBeanOfType(B.class).getProvider().get();
		C c = manager.getBeanOfType(C.class).getProvider().get();
		D d = manager.getBeanOfType(D.class).getProvider().get();
		E e = manager.getBeanOfType(E.class).getProvider().get();

		assertTrue("a.manager vs manager", a.getManager().equals(manager));
		assertTrue("a.a vs a", a.getaInstance().equals(a));
		assertTrue("a.b vs b", a.getbInstance().equals(b));
		assertTrue("a.c vs c", a.getcInstance().equals(c));
		assertTrue("a.d vs d", a.getdInstance().equals(d));

		assertTrue("e.a vs a", e.getA().equals(a));
		assertTrue("e.c vs c", e.getC().equals(c));

	}
}