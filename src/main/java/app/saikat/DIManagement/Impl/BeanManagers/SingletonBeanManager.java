package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.MutableGraph;

import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

public class SingletonBeanManager extends DIBeanManager {

	public SingletonBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph, 
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
	}

	@Override
	public void beanCreated(DIBean<?> bean, Class<?> cls) {
		super.beanCreated(bean, cls);
		((DIBeanImpl<?>) bean).setSingleton(true);
	}
}