package app.saikat.DIManagement.Test_3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import com.google.common.collect.Sets;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for Provider (both Singleton and non singleton)
 */
public class Test_3 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_3", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeansOfType(A.class).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(B.class).iterator().next().getProvider().get();
		C c = manager.getBeansOfType(C.class).iterator().next().getProvider().get();
		D d = manager.getBeansOfType(D.class).iterator().next().getProvider().get();
		Provider<E> eProvider = manager.getBeansOfType(E.class).iterator().next().getProvider();
		F f = manager.getBeansOfType(F.class).iterator().next().getProvider().get();

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A.D vs D", a.getD(), d);
		assertEquals("D.B vs B", d.getB(), b);
		assertEquals("D.C vs C", d.getC(), c);
		assertEquals("F.D vs D", f.getD(), d);

		assertTrue("Number of E instances", E.getNoOfInstances() == 1);

		E e1 = eProvider.get();
		E e2 = eProvider.get();

		assertTrue("Number of E instances", E.getNoOfInstances() == 3);

		Set<Object> allInstances = manager.getObjectMap()
				.get(manager.getBeansOfType(E.class).iterator().next()).parallelStream().map(o -> o.get())
				.filter(o -> o != null).collect(Collectors.toSet());

		Set<Object> expected = Sets.newHashSet(e1,e2,f.getE());

		assertTrue("Checking instances", allInstances.equals(expected));
	}
}