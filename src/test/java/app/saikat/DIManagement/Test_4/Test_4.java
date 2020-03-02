package app.saikat.DIManagement.Test_4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for qualifiers
 */
public class Test_4 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();
		manager.scan("app.saikat.DIManagement.Test_4", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeansOfType(TypeToken.of(A.class)).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(TypeToken.of(B.class), Q1.class).iterator().next().getProvider().get();
		C c = manager.getBeansOfType(TypeToken.of(C.class)).iterator().next().getProvider().get();
		D d0 = manager.getBeansOfType(TypeToken.of(D.class)).iterator().next().getProvider().get();
		D d1 = manager.getBeansOfType(TypeToken.of(D.class), Q1.class).iterator().next().getProvider().get();
		D d2 = manager.getBeansOfType(TypeToken.of(D.class), Q2.class).iterator().next().getProvider().get();
		E e = manager.getBeansOfType(TypeToken.of(E.class)).iterator().next().getProvider().get();
		F f = manager.getBeansOfType(TypeToken.of(F.class), Q2.class).iterator().next().getProvider().get();
		G g = manager.getBeansOfType(TypeToken.of(G.class)).iterator().next().getProvider().get();
		H h = manager.getBeansOfType(TypeToken.of(H.class)).iterator().next().getProvider().get();

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A.C vs C", a.getC(), c);
		assertEquals("B.C vs C", b.getC(), c);
		assertEquals("E.D1 vs D1", e.getD(), d1);
		assertEquals("G.F vs C", g.getF(), f);
		assertEquals("H.D0 vs D0", h.getD0(), d0);
		assertEquals("H.D1 vs D1", h.getD1(), d1);
		assertEquals("H.D2 vs D2", h.getD2(), d2);

		assertEquals("D0 val", d0.getType(), "Type: none");
		assertEquals("D1 val", d1.getType(), "Type: Q1");
		assertEquals("D2 val", d2.getType(), "Type: Q2");
		assertEquals("E PostConstruct", e.getStr(), "Hello world");
	}
}