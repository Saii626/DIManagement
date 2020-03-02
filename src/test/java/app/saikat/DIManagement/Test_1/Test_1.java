package app.saikat.DIManagement.Test_1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Basic test. Check basic dependency resolution and object creation
 */
public class Test_1 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();

		manager.scan("app.saikat.DIManagement.Test_1", "app.saikat.Annotations.DIManagement", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeansOfType(TypeToken.of(A.class)).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(TypeToken.of(B.class)).iterator().next().getProvider().get();
		C c = manager.getBeansOfType(TypeToken.of(C.class)).iterator().next().getProvider().get();
		D d = manager.getBeansOfType(TypeToken.of(D.class)).iterator().next().getProvider().get();
		E e = manager.getBeansOfType(TypeToken.of(E.class)).iterator().next().getProvider().get();

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A.C vs C", a.getC(), c);
		assertEquals("B.C vs C", b.getC(), c);
		assertEquals("B.D vs D", b.getD(), d);
		assertEquals("B.E vs E", b.getE(), e);
		assertEquals("C.D vs D", c.getD(), d);
		assertEquals("D.E vs E", d.getE(), e);
	}
}