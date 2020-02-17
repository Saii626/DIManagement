package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class SingletonBeanManager extends BeanManagerImpl {

	public SingletonBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);
		((DIBeanImpl<?>) bean).setSingleton(true);
	}
}