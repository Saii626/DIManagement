package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Exceptions.NotValidBean;
import app.saikat.DIManagement.Interfaces.DIBean;

public class PostConstructBeanManager extends BeanManagerImpl {

	// Map of parent (enclosing class) bean to set of setter beans
	private Map<DIBean<?>, DIBean<?>> postConstructBeans = new HashMap<>();

	@Override
	public Map<Class<?>, Scan> addToScan() {
		return Collections.singletonMap(PostConstruct.class, createScanObject());
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);

		if (!bean.getProviderType()
				.equals(TypeToken.of(Void.TYPE))) {
			throw new NotValidBean(bean, "PostConstruct should not return value");
		}
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> qualifierAnnotations) {

		if (target.getInvokable()
				.getParameters()
				.size() > 0) {
			throw new NotValidBean(target, "PostConstruct should not have parameters");
		}

		List<DIBean<?>> dependencies = super.resolveDependencies(target, alreadyResolved, toBeResolved, qualifierAnnotations);
		postConstructBeans.put(dependencies.get(0), target);
		return dependencies;
	}

	public DIBean<?> getPostConstructBean(DIBean<?> parent) {
		return postConstructBeans.get(parent);
	}

}