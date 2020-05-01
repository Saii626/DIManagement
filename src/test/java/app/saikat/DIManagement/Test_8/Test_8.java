package app.saikat.DIManagement.Test_8;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIManager;

/**
 * Test for unmanaged annotations
 */
public class Test_8 {

	@Test
	public void test() {

		DIManager manager = DIManager.newInstance();

		manager.scan("app.saikat.DIManagement.Test_8", "app.saikat.DIManagement", "app.saikat.DIManagement.Impl.BeanManagers");

		DIBean<A> bean = manager.getBeansOfType(TypeToken.of(A.class)).iterator().next();

		DIBeanImpl<A> b = (DIBeanImpl<A>) bean;

		assertTrue("No provider created", b.getProviderBean() == null);
	}

}