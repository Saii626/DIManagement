package app.saikat.DIManagement.Test_11;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test to check for injection of providers
 */
public class Test_11 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_11", "app.saikat.Annotations.DIManagement");

		Provider<A> aProvider = manager.getBeanOfType(A.class).getProvider();
		Provider<B> bProvider = manager.getBeanOfType(B.class).getProvider();
		Provider<D> dProvider = manager.getBeanOfType(D.class).getProvider();

		A a = aProvider.get();

		assertTrue("a.bProvider vs bProvider", a.getbProvider().equals(bProvider));
		assertTrue("a.dProvider vs dProvider", a.getdProvider().equals(dProvider));

		assertTrue("No instances of B created", B.getNoOfInstances() == 0);
		assertTrue("No instances of E created", E.getNoOfInstances() == 0);
		assertTrue("No instances of G created", G.getNoOfInstances() == 0);

		D d = dProvider.get();
		assertTrue("5 instances of E created", E.getNoOfInstances() == 5);
		assertTrue("5 instances of G created", G.getNoOfInstances() == 5);

		Set<E> setOfE = d.geteInstances();
		Set<G> setOfG = setOfE.parallelStream().map(e -> e.getG()).collect(Collectors.toSet());

		Set<Object> expectedSetOfE = manager.getImmutableObjectMap().get(manager.getBeanOfType(E.class)).parallelStream().map(w -> w.get()).filter(e -> e != null).collect(Collectors.toSet());
		Set<Object> expectedSetOfG = manager.getImmutableObjectMap().get(manager.getBeanOfType(G.class)).parallelStream().map(w -> w.get()).filter(e -> e != null).collect(Collectors.toSet());

		assertTrue("set of e", setOfE.equals(expectedSetOfE));
		assertTrue("set of g", setOfG.equals(expectedSetOfG));

	}
}