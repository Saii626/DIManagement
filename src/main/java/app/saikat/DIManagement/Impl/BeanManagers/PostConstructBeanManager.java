package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Collection;
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

public class PostConstructBeanManager extends DIBeanManager {

	// Map of parent (enclosing class) bean to set of setter beans
	private Map<DIBean<?>, DIBean<?>> postConstructBeans = new HashMap<>();

	public PostConstructBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph, 
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		super(results, mutableGraph, objectMap, helper);
	}

	@Override
	public void beanCreated(DIBean<?> bean, Class<?> cls) {
		super.beanCreated(bean, cls);

		if (bean.get().containsRight() && !bean.getProviderType().equals(Void.TYPE)) {
			throw new NotValidBean(bean, "PostConstruct should not return value");
		}
	}

	@Override
	public List<DIBean<?>> resolveDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		if (target.get().getRight().get().getParameterCount() > 0) {
			throw new NotValidBean(target, "PostConstruct should not have parameters");
		}

		List<DIBean<?>> dependencies = super.resolveDependencies(target, alreadyResolved, toBeResolved);
		postConstructBeans.put(dependencies.get(0), target);
		return dependencies;
	}

	public DIBean<?> getPostConstructBean(DIBean<?> parent) {
		return postConstructBeans.get(parent);
	}

}