package app.saikat.DIManagement.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Set;

import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

public class SingletonBeanManager implements DIBeanManager {

	@Override
	public void beanCreated(DIBean<?> bean, Results results) {
		DIBeanImpl<?> b = (DIBeanImpl<?>) bean;
		b.setSingleton(true);
	}

	@Override
	public void beanCreatedNewInstance(DIBean<?> bean, Object instance, Set<WeakReference<Object>> allInstances) {
	}

	@Override
	public void beanProviderCreated(DIBean<?> bean, Results results) {
	}

	@Override
	public void beanDependencyResolved(DIBean<?> bean) {
	}

}