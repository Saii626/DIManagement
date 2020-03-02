package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.ExternalImpl.ProviderImpl;
import app.saikat.DIManagement.Impl.Helpers.DependencyHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;

public abstract class BeanManagerImpl extends DIBeanManager {

	public boolean shouldResolveDependency() {
		return true;
	}

	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> allQualifiers) {

		if (!(target instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(String
					.format("Dont know how to resolve dependency of %s, as it is not instance of DIBeanImpl.class", target
							.toString()));
		}

		DIBeanImpl<?> t = (DIBeanImpl<?>) target;
		logger.debug("Scanning dependencies of {}", target);
		List<DIBean<?>> unresolvedDependencies = DependencyHelper.scanAndSetDependencies(t, allQualifiers);
		logger.debug("Unresolved dependencies of {}: {}", target, unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper
				.resolveAndSetDependencies(t, alreadyResolved, toBeResolved);
		logger.debug("Resolved dependencies of {}: {}", target, resolvedDependencies);

		return resolvedDependencies;
	}

	public boolean shouldCreateProvider() {
		return true;
	}

	@Override
	@SuppressWarnings("serial")
	public <T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target,
			InjectBeanManager injectBeanManager, PostConstructBeanManager postConstructBeanManager) {
		if (!(target instanceof DIBeanImpl)) {

			throw new RuntimeException(String
					.format("Dont know how to create provider of %s, as it is not instance of DIBeanImpl.class", target
							.toString()));
		}

		DIBeanImpl<T> t = (DIBeanImpl<T>) target;
		ProviderImpl<T> provider = new ProviderImpl<>(t, injectBeanManager, postConstructBeanManager);

		TypeToken<Provider<T>> providerType = new TypeToken<Provider<T>>() {}
				.where(new TypeParameter<T>() {}, t.getProviderType()
						.wrap());

		ConstantProviderBean<Provider<T>> providerBean = new ConstantProviderBean<>(providerType,
				target.getQualifier());
		providerBean.setProvider(() -> provider);
		t.setProviderBean(providerBean);

		logger.debug("ProviderBean {} created and set for {}", providerBean, target);

		// Add provider bean to results
		this.repo.addBean(providerBean);

		return providerBean;
	}
}