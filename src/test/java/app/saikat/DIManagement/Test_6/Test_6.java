package app.saikat.DIManagement.Test_6;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.reflect.Invokable;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIManager;


/**
 * Test for custom method annotations
 */
public class Test_6 {

	@Test
	public void generateObjects_6() throws NoSuchMethodException, SecurityException {
		DIManager manager = DIManager.newInstance();

		manager.scan("app.saikat.DIManagement.Test_6", "app.saikat.DIManagement.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");

		A a = manager.getBeansOfType(TypeToken.of(A.class)).iterator().next().getProvider().get();
		B b = manager.getBeansOfType(TypeToken.of(B.class)).iterator().next().getProvider().get();
		C c = manager.getBeansOfType(TypeToken.of(C.class)).iterator().next().getProvider().get();
		D d = manager.getBeansOfType(TypeToken.of(D.class)).iterator().next().getProvider().get();

		// Invoke the annotated functions once
		manager.getBeansWithType(MethodAnnot_1.class).forEach(bean -> bean.getProvider().get());
		manager.getBeansWithType(MethodAnnot_2.class).forEach(bean -> bean.getProvider().get());

		Set<Invokable<Object, ?>> actual = manager.getBeansWithType(MethodAnnot_3.class).parallelStream()
				.map(bean -> bean.getInvokable()).collect(Collectors.toSet());

		Set<Invokable<?, ?>> expected = new HashSet<>();
		expected.add(Invokable.from(C.class.getMethod("setE", E.class)));
		expected.add(Invokable.from(E.class.getMethod("testFunc")));

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A state", a.getStr(), "State 2");
		assertEquals("C.D vs D", c.getD(), d);
		assertEquals("C.E vs E", c.getE(), null);
		assertEquals("MethodAmmot_3 annoted classes", actual, expected);
	}
}