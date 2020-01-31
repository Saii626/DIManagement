package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.MutableGraph;

import app.saikat.DIManagement.Exceptions.NotValidBean;
import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.Utils.CommonFunc;

public class InjectBeanManager extends DIBeanManager {

	// Map of parent (enclosing class) bean to set of setter beans
	private Map<DIBean<?>, Set<DIBean<?>>> setterInjectBeans = new HashMap<>();

	public InjectBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph, 
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
	}

	@Override
	public void beanCreated(DIBean<?> bean, Class<?> type) {
		super.beanCreated(bean, type);

		if (bean.get().containsRight() && !bean.getProviderType().equals(Void.TYPE)) {
			throw new NotValidBean(bean, "Setter injection should not return value");
		} 
	}


	@Override
	public List<DIBean<?>> resolveDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {
		List<DIBean<?>> deps = super.resolveDependencies(target, alreadyResolved, toBeResolved);

		if (target.get().containsRight()) {
			CommonFunc.addToMapSet(setterInjectBeans, deps.get(0), target);
		}

		return deps;
	}

	public Set<DIBean<?>> getSetterInjectionsFor(DIBean<?> parent) {
		Set<DIBean<?>> setterBeans = setterInjectBeans.get(parent);
		return setterBeans != null ? setterBeans : Collections.emptySet();
	}
}