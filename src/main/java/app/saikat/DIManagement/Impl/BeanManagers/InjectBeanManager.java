package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.DIManagement.Exceptions.NotValidBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.PojoCollections.Utils.CommonFunc;

public class InjectBeanManager extends BeanManagerImpl {

	// Map of parent (enclosing class) bean to set of setter beans
	private Map<DIBean<?>, Set<DIBean<?>>> setterInjectBeans = new HashMap<>();

	@Override
	public Map<Class<?>, Scan> addToScan() {
		return Collections.singletonMap(Inject.class, createScanObject());
	}

	@Override
	public <T> void beanCreated(DIBean<T> bean) {
		super.beanCreated(bean);

		if (!(bean instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(String
					.format("Wrorng bean type for Inject bean. Expected type DIBeanImpl.class found %s", bean.getClass()
							.getSimpleName()));
		}

		if (((DIBeanImpl<?>) bean).isMethod() && !bean.getProviderType()
				.equals(TypeToken.of(Void.TYPE))) {
			throw new NotValidBean(bean, "Setter injection should not return value");
		}
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> qualifierAnnotations){

		if (!(target instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(
					String.format("Wrorng bean type for Inject bean. Expected type DIBeanImpl.class found %s", target
							.getClass()
							.getSimpleName()));
		}

		List<DIBean<?>> deps = super.resolveDependencies(target, alreadyResolved, toBeResolved, qualifierAnnotations);

		if (((DIBeanImpl<?>) target).isMethod()) {
			CommonFunc.addToMapSet(setterInjectBeans, deps.get(0), target);
		}

		return deps;
	}

	public Set<DIBean<?>> getSetterInjectionsFor(DIBean<?> parent) {
		Set<DIBean<?>> setterBeans = setterInjectBeans.get(parent);
		return setterBeans != null ? setterBeans : Collections.emptySet();
	}
}