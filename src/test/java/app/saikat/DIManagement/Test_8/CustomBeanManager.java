package app.saikat.DIManagement.Test_8;

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

public class CustomBeanManager extends DIBeanManager {

	public CustomBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph,
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
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

}