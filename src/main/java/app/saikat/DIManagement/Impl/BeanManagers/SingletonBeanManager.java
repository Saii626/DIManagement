package app.saikat.DIManagement.Impl.BeanManagers;

import java.util.Collections;
import java.util.Map;

import javax.inject.Singleton;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;

public class SingletonBeanManager extends BeanManagerImpl {

	@Override
	public Map<Class<?>, Scan> addToScan() {
		return Collections.singletonMap(Singleton.class, createScanObject());
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);
		((DIBeanImpl<?>) bean).setSingleton(true);
	}
}