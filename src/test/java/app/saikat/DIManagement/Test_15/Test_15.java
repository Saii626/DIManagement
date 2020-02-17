package app.saikat.DIManagement.Test_15;

import static org.junit.Assert.assertTrue;

import javax.inject.Provider;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for proper handling of generic parameters
 */
public class Test_15 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_15", "app.saikat.DIManagement.Annotations",
				"app.saikat.DIManagement.Impl.BeanManagers");

		B b = manager.getBeanOfType(B.class).getProvider().get();
		C c = manager.getBeanOfType(C.class).getProvider().get();
		D d = manager.getBeanOfType(D.class).getProvider().get();
		E e = manager.getBeanOfType(E.class).getProvider().get();
		G g = manager.getBeanOfType(G.class).getProvider().get();
		H h = manager.getBeanOfType(H.class).getProvider().get();

		A<B, C, E> t1 = g.getT1();
		A<E, B, C> t2 = g.getT2();
		A<D, B, E> t3 = g.getT3();
		A<C, E, D> t4 = g.getT4();
		A<B, D, B> t5 = g.getT5();

		Provider<A<B, C, E>> p1 = h.getP1();
		Provider<A<E, B, C>> p2 = h.getP2();
		Provider<A<D, B, E>> p3 = h.getP3();
		Provider<A<C, E, D>> p4 = h.getP4();
		Provider<A<B, D, B>> p5 = h.getP5();

		assertTrue("g.t1.1 vs b", t1.getP().equals(b));
		assertTrue("g.t1.2 vs c", t1.getQ().equals(c));
		assertTrue("g.t1.3 vs e", t1.getR().equals(e));

		assertTrue("g.t2.1 vs e", t2.getP().equals(e));
		assertTrue("g.t2.2 vs b", t2.getQ().equals(b));
		assertTrue("g.t2.3 vs c", t2.getR().equals(c));

		assertTrue("g.t3.1 vs d", t3.getP().equals(d));
		assertTrue("g.t3.2 vs b", t3.getQ().equals(b));
		assertTrue("g.t3.3 vs e", t3.getR().equals(e));

		assertTrue("g.t4.1 vs c", t4.getP().equals(c));
		assertTrue("g.t4.2 vs e", t4.getQ().equals(e));
		assertTrue("g.t4.3 vs d", t4.getR().equals(d));

		assertTrue("g.t5.1 vs b", t5.getP().equals(b));
		assertTrue("g.t5.2 vs d", t5.getQ().equals(d));
		assertTrue("g.t5.3 vs b", t5.getR().equals(b));

		assertTrue("h.p1.get va t1", p1.get().equals(t1));
		assertTrue("h.p2.get va t2", p2.get().equals(t2));
		assertTrue("h.p3.get va t3", p3.get().equals(t3));
		assertTrue("h.p4.get va t4", p4.get().equals(t4));
		assertTrue("h.p5.get va t5", p5.get().equals(t5));
	}
}