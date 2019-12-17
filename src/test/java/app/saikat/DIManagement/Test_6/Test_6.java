package app.saikat.DIManagement.Test_6;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIManager;


/**
 * Test for custom method annotations
 */
public class Test_6 {

	@Test
	public void generateObjects_6() throws NoSuchMethodException, SecurityException {
		DIManager manager = DIManager.newInstance();

		manager.initialize("app.saikat.DIManagement.Test_6", "app.saikat.DIManagement.Annotations");

		A a = manager.getBeansOfType(A.class).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(B.class).iterator().next().getProvider().get();
		C c = manager.getBeansOfType(C.class).iterator().next().getProvider().get();
		D d = manager.getBeansOfType(D.class).iterator().next().getProvider().get();

		// Invoke the annotated functions once
		manager.getBeansAnnotatedWith(MethodAnnot_1.class).forEach(bean -> bean.getProvider().get());
		manager.getBeansAnnotatedWith(MethodAnnot_2.class).forEach(bean -> bean.getProvider().get());

		Set<Method> actual = manager.getBeansAnnotatedWith(MethodAnnot_3.class).parallelStream()
				.map(bean -> ((DIBeanImpl<?>) bean).get().getRight().get()).collect(Collectors.toSet());

		Set<Method> expected = new HashSet<>();
		expected.add(C.class.getMethod("setE", E.class));
		expected.add(E.class.getMethod("testFunc"));

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A state", a.getStr(), "State 2");
		assertEquals("C.D vs D", c.getD(), d);
		assertEquals("C.E vs E", c.getE(), null);
		assertTrue("MethodAmmot_3 annoted classes", actual.equals(expected));
	}
}