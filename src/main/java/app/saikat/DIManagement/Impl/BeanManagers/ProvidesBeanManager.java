package app.saikat.DIManagement.Impl.BeanManagers;

import java.util.Collections;
import java.util.Map;

import app.saikat.Annotations.DIManagement.Provides;
import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;

public class ProvidesBeanManager extends BeanManagerImpl {

	@Override
	public Map<Class<?>, Scan> addToScan() {
		return Collections.singletonMap(Provides.class, createScanObject());
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);
		DIBeanImpl<?> b = (DIBeanImpl<?>) bean;

		b.setSingleton(b.getInvokable()
				.getAnnotation(Provides.class)
				.singleton());
	}

}