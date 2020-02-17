package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Exceptions.NotValidBean;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.Results;

public class PostConstructBeanManager extends BeanManagerImpl {

	// Map of parent (enclosing class) bean to set of setter beans
	private Map<DIBean<?>, DIBean<?>> postConstructBeans = new HashMap<>();

	public PostConstructBeanManager(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		super(results, objectMap, helper);
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);

		if (!bean.getProviderType().equals(TypeToken.of(Void.TYPE))) {
			throw new NotValidBean(bean, "PostConstruct should not return value");
		}
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		if (target.getInvokable().getParameters().size() > 0) {
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