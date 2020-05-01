package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Interfaces.DIBean;

public class NoOpBeanManager extends BeanManagerImpl {

	@Override
	public <T> void beanCreated(DIBean<T> bean) {}

	@Override
	public void scanComplete() {}

	@Override
	public boolean shouldResolveDependency() {
		return false;
	}

	@Override
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> allQualifiers) {
		return null;
	}

	@Override
	public boolean shouldCreateProvider() {
		return false;
	}

	@Override
	public <T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target,
			InjectBeanManager injectBeanManager, PostConstructBeanManager postConstructBeanManager) {
		return null;
	}

	@Override
	public <T> void newInstanceCreated(DIBean<T> bean, T instance) {}

}