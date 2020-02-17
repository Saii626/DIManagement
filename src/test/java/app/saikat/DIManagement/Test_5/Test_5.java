package app.saikat.DIManagement.Test_5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import app.saikat.DIManagement.Interfaces.DIManager;


/**
 * Test for custom class annotations
 */
public class Test_5 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();
		manager.initialize("app.saikat.DIManagement.Test_5", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeansOfType(A.class, ClassAnnot_1.class).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(B.class).iterator().next().getProvider().get();

		Set<Class<?>> actual = manager.getBeansAnnotatedWith(ClassAnnot_2.class).parallelStream()
				.map(bean -> bean.getProviderType().getRawType()).collect(Collectors.toSet());
		Set<Class<?>> expectedClasses = new HashSet<>();
		expectedClasses.add(C.class);
		expectedClasses.add(E.class);

		assertEquals("A.B vs B", a.getB(), b);
		assertTrue("ClassAnnot_2 annoted classes", actual.equals(expectedClasses));
		assertEquals("Class C not instantiated", C.getState(), "Not instanciated");
	}
}