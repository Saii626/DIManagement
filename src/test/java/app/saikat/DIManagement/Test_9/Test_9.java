package app.saikat.DIManagement.Test_9;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Provider;

import com.google.common.collect.Sets;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for Inject annotation
 */
public class Test_9 {

	@Test
	public void test(){
		DIManager manager = DIManager.newInstance();

		manager.initialize("app.saikat.DIManagement.Test_9", "app.saikat.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		DIBean<A> aBean = manager.getBeansOfType(A.class).iterator().next();
		Provider<A> provider = aBean.getProvider();

		assertTrue("A was not created", A.getNoOfInstances() == 0);
		assertTrue("B was not created", B.getNoOfInstances() == 0);
		assertTrue("C was not created", C.getNoOfInstances() == 0);
		
		A a1 = provider.get();
		A a2 = provider.get();
		A a3 = provider.get();
		C c = manager.getBeansOfType(C.class).iterator().next().getProvider().get();

		assertTrue("3 instances of A created", A.getNoOfInstances() == 3);
		assertTrue("B was not created", B.getNoOfInstances() == 0);
		assertTrue("1 instances of C created", C.getNoOfInstances() == 1);

		assertTrue("a1 c instance", a1.getC().equals(c));
		assertTrue("a2 c instance", a2.getC().equals(c));
		assertTrue("a3 c instance", a3.getC().equals(c));

		Set<Object> actual = Sets.newHashSet(a1, a2, a3);

		System.out.println("Objects: " + manager.getObjectMap().size());
		Set<Object> expected = manager.getObjectMap().get(aBean).parallelStream().map(o -> o.get()).filter(o -> o!= null).collect(Collectors.toSet());

		assertTrue("All objects are created correctly", actual.equals(expected));
	}
}