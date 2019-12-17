package app.saikat.DIManagement.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Set;

import app.saikat.Annotations.DIManagement.Provides;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

public class ProvidesBeanManager implements DIBeanManager {

	@Override
	public void beanCreated(DIBean<?> bean, Results results) {
		DIBeanImpl<?> b = (DIBeanImpl<?>) bean;

		b.setSingleton(b.get()
				.apply(c -> c.getDeclaringClass().getAnnotation(Provides.class), m -> m.getAnnotation(Provides.class))
				.singleton());

	}

	@Override
	public void beanProviderCreated(DIBean<?> bean, Results results) {
	}

	@Override
	public void beanCreatedNewInstance(DIBean<?> bean, Object instance, Set<WeakReference<Object>> allInstances) {
	}

	@Override
	public void beanDependencyResolved(DIBean<?> bean) {
	}

}