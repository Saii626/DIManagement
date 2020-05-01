package app.saikat.DIManagement.Test_2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.util.List;

import com.google.common.collect.Lists;

import org.junit.Test;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Exceptions.CircularDependencyException;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIManager;

public class Test_2 {

	@Test
	public void test() {
		DIManager manager = DIManager.newInstance();

		try {
			manager.scan("app.saikat.DIManagement.Test_2", "app.saikat.Annotations", "app.saikat.DIManagement.Impl.BeanManagers");
		} catch (CircularDependencyException e) {
			DIBean<B> bBean = manager.getBeansOfType(TypeToken.of(B.class)).iterator().next();
			DIBean<D> dBean = manager.getBeansOfType(TypeToken.of(D.class)).iterator().next();

			// target, dependent
			List<DIBean<?>> t1 = Lists.newArrayList(bBean, dBean);
			List<DIBean<?>> t2 = Lists.newArrayList(dBean, bBean);

			List<DIBean<?>> actual = Lists.newArrayList(e.getTarget(), e.getDependent());

			assertTrue("Found circular dependency target: " + e.getTarget() + " dependent: " + e.getDependent(), actual.equals(t1) || actual.equals(t2));
			assumeNoException(e);
		}
	}
}