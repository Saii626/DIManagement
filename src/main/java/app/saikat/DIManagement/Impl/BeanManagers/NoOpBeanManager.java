package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class NoOpBeanManager extends BeanManagerImpl {

	public NoOpBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
	}

	@Override
	public void scanComplete() {
	}

	@Override
	public boolean shouldResolveDependency() {
		return false;
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {
		return null;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}

	@Override
	public <T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target) {
		return null;
	}

	@Override
	public void newInstanceCreated(DIBean<?> bean, Object instance) {
	}

}