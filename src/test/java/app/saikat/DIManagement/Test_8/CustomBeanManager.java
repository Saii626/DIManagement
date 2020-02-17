package app.saikat.DIManagement.Test_8;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.saikat.DIManagement.Impl.BeanManagers.BeanManagerImpl;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class CustomBeanManager extends BeanManagerImpl {

	public CustomBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
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

}