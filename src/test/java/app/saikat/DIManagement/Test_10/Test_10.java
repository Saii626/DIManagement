package app.saikat.DIManagement.Test_10;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Exceptions.BeanNotFoundException;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 *  Test for injection of DIManager
 */
public class Test_10 {

	@Test
	public void test() throws BeanNotFoundException {
		DIManager manager = DIManager.newInstance();

		manager.scan("app.saikat.DIManagement.Test_10", "app.saikat.Annotations.DIManagement", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeanOfType(TypeToken.of(A.class)).getProvider().get();
		B b = manager.getBeanOfType(TypeToken.of(B.class)).getProvider().get();
		C c = manager.getBeanOfType(TypeToken.of(C.class)).getProvider().get();
		D d = manager.getBeanOfType(TypeToken.of(D.class)).getProvider().get();
		E e = manager.getBeanOfType(TypeToken.of(E.class)).getProvider().get();

		assertTrue("a.manager vs manager", a.getManager().equals(manager));
		assertTrue("a.a vs a", a.getaInstance().equals(a));
		assertTrue("a.b vs b", a.getbInstance().equals(b));
		assertTrue("a.c vs c", a.getcInstance().equals(c));
		assertTrue("a.d vs d", a.getdInstance().equals(d));

		assertTrue("e.a vs a", e.getA().equals(a));
		assertTrue("e.c vs c", e.getC().equals(c));

	}
}