package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.MutableGraph;

import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;

public class NoOpBeanManager extends DIBeanManager {

	public NoOpBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph, 
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
	}

	@Override
	public void beanCreated(DIBean<?> bean, Class<?> cls) {
	}

	@Override
	public void scanComplete() {
	}

	@Override
	public boolean shouldResolveDependency() {
		return false;
	}
	@Override
	public List<DIBean<?>> resolveDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {
		return null;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}

	@Override
	public void newInstanceCreated(DIBean<?> bean, Object instance) {
	}

}