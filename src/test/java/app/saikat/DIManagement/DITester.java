package app.saikat.DIManagement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import app.saikat.DIManagement.Configurations.ClassAnnotationConfig;
import app.saikat.DIManagement.Configurations.MethodAnnotationConfig;
import app.saikat.DIManagement.Configurations.ScanConfig;
import app.saikat.DIManagement.Exceptions.CircularDependencyException;
import app.saikat.DIManagement.Exceptions.ClassNotUnderDIException;

public class DITester {

	// @Test
	// public void tet_() throws NoSuchMethodException, SecurityException {
	// 	// DIManager.initialize(ScanConfig.newBuilder().addPackagesToScan("app.saikat.DIManagement.Test_").build());
	// 	Method m = A.class.getDeclaredMethod("test", Provider.class);
	// 	ParameterizedType t = (ParameterizedType) m.getGenericParameterTypes()[0];
	// 	System.out.println(t.getActualTypeArguments()[0].getClass());
	// }
	@Test
	public void generateObjects_1() throws ClassNotUnderDIException {
		DIManager.initialize(ScanConfig.newBuilder()
				.addPackagesToScan("app.saikat.DIManagement.Test_1")
				.build());

		app.saikat.DIManagement.Test_1.A a = DIManager.get(app.saikat.DIManagement.Test_1.A.class);
		app.saikat.DIManagement.Test_1.B b = DIManager.get(app.saikat.DIManagement.Test_1.B.class);
		app.saikat.DIManagement.Test_1.C c = DIManager.get(app.saikat.DIManagement.Test_1.C.class);
		app.saikat.DIManagement.Test_1.D d = DIManager.get(app.saikat.DIManagement.Test_1.D.class);
		app.saikat.DIManagement.Test_1.E e = DIManager.get(app.saikat.DIManagement.Test_1.E.class);

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A.C vs C", a.getC(), c);
		assertEquals("B.C vs C", b.getC(), c);
		assertEquals("B.D vs D", b.getD(), d);
		assertEquals("B.E vs E", b.getE(), e);
		assertEquals("C.D vs D", c.getD(), d);
		assertEquals("D.E vs E", d.getE(), e);

	}

	@Test
	public void generateObjects_2() throws ClassNotUnderDIException {
		try {
			DIManager.initialize(ScanConfig.newBuilder()
					.addPackagesToScan("app.saikat.DIManagement.Test_2")
					.build());
		} catch (CircularDependencyException e) {
			assumeNoException(e);
		}
		app.saikat.DIManagement.Test_2.A a = DIManager.get(app.saikat.DIManagement.Test_2.A.class);

		assertEquals("Circular dependency", a, null);
	}

	@Test
	public void generateObjects_3() throws ClassNotUnderDIException {
		DIManager.initialize(ScanConfig.newBuilder()
				.addPackagesToScan("app.saikat.DIManagement.Test_3")
				.build());

		app.saikat.DIManagement.Test_3.A a = DIManager.get(app.saikat.DIManagement.Test_3.A.class);
		app.saikat.DIManagement.Test_3.B b = DIManager.get(app.saikat.DIManagement.Test_3.B.class);
		app.saikat.DIManagement.Test_3.C c = DIManager.get(app.saikat.DIManagement.Test_3.C.class);
		app.saikat.DIManagement.Test_3.D d = DIManager.get(app.saikat.DIManagement.Test_3.D.class);
		app.saikat.DIManagement.Test_3.E e = DIManager.get(app.saikat.DIManagement.Test_3.E.class);
		app.saikat.DIManagement.Test_3.F f = DIManager.get(app.saikat.DIManagement.Test_3.F.class);

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A.D vs D", a.getD(), d);
		assertEquals("B.E vs E", b.getE(), e);
		assertEquals("D.B vs B", d.getB(), b);
		assertEquals("D.C vs C", d.getC(), c);
		assertEquals("F.D vs D", f.getD(), d);
		assertEquals("F.E vs E", f.getE(), e);
	}

	@Test
	public void generateObjects_4() throws ClassNotUnderDIException {
		DIManager.initialize(ScanConfig.newBuilder()
				.addPackagesToScan("app.saikat.DIManagement.Test_4")
				.build());

		app.saikat.DIManagement.Test_4.A a = DIManager.get(app.saikat.DIManagement.Test_4.A.class);
		app.saikat.DIManagement.Test_4.B b = DIManager.get(app.saikat.DIManagement.Test_4.B.class,
				app.saikat.DIManagement.Test_4.Q1.class);
		app.saikat.DIManagement.Test_4.C c = DIManager.get(app.saikat.DIManagement.Test_4.C.class);
		app.saikat.DIManagement.Test_4.D d0 = DIManager.get(app.saikat.DIManagement.Test_4.D.class);
		app.saikat.DIManagement.Test_4.D d1 = DIManager.get(app.saikat.DIManagement.Test_4.D.class,
				app.saikat.DIManagement.Test_4.Q1.class);
		app.saikat.DIManagement.Test_4.D d2 = DIManager.get(app.saikat.DIManagement.Test_4.D.class,
				app.saikat.DIManagement.Test_4.Q2.class);
		app.saikat.DIManagement.Test_4.E e = DIManager.get(app.saikat.DIManagement.Test_4.E.class);
		app.saikat.DIManagement.Test_4.F f = DIManager.get(app.saikat.DIManagement.Test_4.F.class,
				app.saikat.DIManagement.Test_4.Q2.class);
		app.saikat.DIManagement.Test_4.G g = DIManager.get(app.saikat.DIManagement.Test_4.G.class);
		app.saikat.DIManagement.Test_4.H h = DIManager.get(app.saikat.DIManagement.Test_4.H.class);

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

	@Test
	public void generateObjects_5() throws ClassNotUnderDIException {
		ScanConfig config = ScanConfig.newBuilder()
				.addAnnotationConfig(ClassAnnotationConfig.getBuilder()
						.forAnnotation(app.saikat.DIManagement.Test_5.ClassAnnot_1.class)
						.autoBuild(true)
						.checkDependency(true)
						.build())
				.addAnnotationConfig(ClassAnnotationConfig.getBuilder()
						.forAnnotation(app.saikat.DIManagement.Test_5.ClassAnnot_2.class)
						.autoBuild(false)
						.checkDependency(false)
						.build())
				.addPackagesToScan("app.saikat.DIManagement.Test_5")
				.build();

		DIManager.initialize(config);

		app.saikat.DIManagement.Test_5.A a = DIManager.get(app.saikat.DIManagement.Test_5.A.class,
				app.saikat.DIManagement.Test_5.ClassAnnot_1.class);
		app.saikat.DIManagement.Test_5.B b = DIManager.get(app.saikat.DIManagement.Test_5.B.class);

		Set<Class<?>> actual = new HashSet<>(
				DIManager.getAnnotatedClasses(app.saikat.DIManagement.Test_5.ClassAnnot_2.class));
		Set<Class<?>> expectedClasses = new HashSet<>();
		expectedClasses.add(app.saikat.DIManagement.Test_5.C.class);
		expectedClasses.add(app.saikat.DIManagement.Test_5.E.class);

		assertEquals("A.B vs B", a.getB(), b);
		assertTrue("ClassAnnot_2 annoted classes", actual.equals(expectedClasses));
		assertEquals("Class C not instantiated", app.saikat.DIManagement.Test_5.C.getState(), "Not instanciated");
	}

	@Test
	public void generateObjects_6() throws ClassNotUnderDIException, NoSuchMethodException, SecurityException {
		ScanConfig config = ScanConfig.newBuilder()
				.addAnnotationConfig(MethodAnnotationConfig.getBuilder()
						.forAnnotation(app.saikat.DIManagement.Test_6.MethodAnnot_1.class)
						.autoBuild(true)
						.checkDependency(false)
						.autoInvoke(true)
						.build())
				.addAnnotationConfig(MethodAnnotationConfig.getBuilder()
						.forAnnotation(app.saikat.DIManagement.Test_6.MethodAnnot_2.class)
						.autoBuild(true)
						.checkDependency(true)
						.autoInvoke(true)
						.build())
				.addAnnotationConfig(MethodAnnotationConfig.getBuilder()
						.forAnnotation(app.saikat.DIManagement.Test_6.MethodAnnot_3.class)
						.autoBuild(false)
						.checkDependency(true)
						.autoInvoke(false)
						.build())
				.addPackagesToScan("app.saikat.DIManagement.Test_6")
				.build();

		DIManager.initialize(config);

		app.saikat.DIManagement.Test_6.A a = DIManager.get(app.saikat.DIManagement.Test_6.A.class);
		app.saikat.DIManagement.Test_6.B b = DIManager.get(app.saikat.DIManagement.Test_6.B.class);
		app.saikat.DIManagement.Test_6.C c = DIManager.get(app.saikat.DIManagement.Test_6.C.class);
		app.saikat.DIManagement.Test_6.D d = DIManager.get(app.saikat.DIManagement.Test_6.D.class);

		Set<Method> actual = new HashSet<>(DIManager.getAnnotatedMethods(app.saikat.DIManagement.Test_6.MethodAnnot_3.class));
		Set<Method> expected = new HashSet<>();
		expected.add(app.saikat.DIManagement.Test_6.C.class.getMethod("setE", app.saikat.DIManagement.Test_6.E.class));
		expected.add(app.saikat.DIManagement.Test_6.E.class.getMethod("testFunc"));

		assertEquals("A.B vs B", a.getB(), b);
		assertEquals("A state", a.getStr(), "State 2");
		assertEquals("C.D vs D", c.getD(), d);
		assertEquals("C.E vs E", c.getE(), null);
		assertTrue("MethodAmmot_3 annoted classes", actual.equals(expected));
	}
}