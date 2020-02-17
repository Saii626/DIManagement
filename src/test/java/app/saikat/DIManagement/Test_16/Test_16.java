package app.saikat.DIManagement.Test_16;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import app.saikat.Annotations.DIManagement.Generator;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for proper handling of primitive types
 */
public class Test_16 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_16", "app.saikat.DIManagement.Annotations",
				"app.saikat.DIManagement.Impl.BeanManagers");

		B b = manager.getBeanOfType(B.class).getProvider().get();
		D d = manager.getBeanOfType(D.class).getProvider().get();
		E e = manager.getBeanOfType(E.class).getProvider().get();

		assertTrue("d.q1 vs q1", d.getQ1() == 100);
		assertTrue("d.q2 vs q2", d.getQ2() == 200);

		assertEquals("d.d1 vs d1", d.getD1(), 10.101010101010, 10e-6);
		assertEquals("d.d2 vs d2", d.getD2(), 20.202020202020, 10e-6);

		Generator<Boolean> g1 = e.getG1();
		Generator<Boolean> g2 = e.getG2();
		Generator<C> gC = e.getG3();

		boolean g11 = g1.generate(10);
		boolean g12 = g1.generate(5);

		boolean g21 = g2.generate(10);
		boolean g22 = g2.generate(5);

		C c = gC.generate(15l, (byte) 12);

		assertTrue("g11 value", g11 == true);
		assertTrue("g12 value", g12 == true);
		assertTrue("g21 value", g21 == false);
		assertTrue("g22 value", g22 == false);

		assertTrue("c.l vs l", c.getL() == 15);
		assertTrue("c.by vs by", c.getBy() == 12);
		assertEquals("c.d vs d", c.getD(), 10.101010101010, 10e-6);
		assertTrue("c.b vs b", c.getB().equals(b));
	}
}