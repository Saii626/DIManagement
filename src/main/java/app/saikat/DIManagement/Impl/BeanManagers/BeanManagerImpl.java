package app.saikat.DIManagement.Impl.BeanManagers;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.DIBeans.DIBeanImpl;
import app.saikat.DIManagement.Impl.ExternalImpl.ProviderImpl;
import app.saikat.DIManagement.Impl.Helpers.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.Helpers.DependencyHelper;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.Results;
import app.saikat.PojoCollections.Utils.CommonFunc;

public abstract class BeanManagerImpl implements DIBeanManager {

	protected final Results results;
	protected final DIBeanManagerHelper helper;
	protected final Map<DIBean<?>, Set<WeakReference<?>>> objectMap;

	public BeanManagerImpl(Results results, Map<DIBean<?>, Set<WeakReference<?>>> objectMap,
			DIBeanManagerHelper helper) {
		this.results = results;
		this.objectMap = objectMap;
		this.helper = helper;
	}

	@Override
	public Map<Class<? extends Annotation>, ScanAnnotation> addAnnotationsToScan() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Class<?>, ScanInterface> addInterfacessToScan() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Class<?>, ScanSubClass> addSubClassesToScan() {
		return Collections.emptyMap();
	}

	/**
	 * Callback called after a bean has been created
	 * @param bean bean that has been created
	 */
	public <T> void beanCreated(DIBean<T> bean) {
		switch (bean.getBeanType()) {
		case ANNOTATION:
			results.addAnnotationBean(bean);
			break;
		case INTERFACE:
			results.addInterfaceBean(bean, bean.getSuperClass());
			break;
		case SUBCLASS:
			results.addSubclassBean(bean, bean.getSuperClass());
			break;
		case GENERATED:
			results.addGeneratedBean(bean);
		}
	}

	/**
	 * Callback called after all scan has been done. Create additional beans in this
	 * if necessary
	 */
	public void scanComplete() {}

	/**
	 * If dependenies of the bean scould be scanned and resolved
	 * @return true if the bean should be scanned and resolved, else false
	 */
	public boolean shouldResolveDependency() {
		return true;
	}

	/**
	 * Method called to scan and resolve dependencies of the target bean. This also
	 * sets the dependencies of the bean and adds them to dependency graph
	 * @param target the bean whose dependencies need to be resolved
	 * @param alreadyResolved collection of already resolved beans
	 * @param toBeResolved collection of yet to be resolved beans
	 * @return list of resolved dependencies of the bean
	 */
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		if (!(target instanceof DIBeanImpl<?>)) {
			throw new RuntimeException(String
					.format("Dont know how to resolve dependency of %s, as it is not instance of DIBeanImpl.class", target
							.toString()));
		}

		DIBeanImpl<?> t = (DIBeanImpl<?>) target;
		logger.debug("Scanning dependencies of {}", target);
		List<DIBean<?>> unresolvedDependencies = DependencyHelper.scanAndSetDependencies(t, results);
		logger.debug("Unresolved dependencies of {}: {}", target, unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper
				.resolveAndSetDependencies(t, alreadyResolved, toBeResolved);
		logger.debug("Resolved dependencies of {}: {}", target, resolvedDependencies);

		// resolvedDependencies.forEach(dep -> checkAndAddPair(target, dep));
		// mutableGraph.addNode(target);

		return resolvedDependencies;
	}

	/**
	 * Callback called after all dependencies of beans have been resolved
	 */
	public void dependencyResolved() {}

	/**
	 * If provider of the bean scould be scanned and resolved
	 * @return true if the bean should be scanned and resolved, else false
	 */
	public boolean shouldCreateProvider() {
		return true;
	}

	@Override
	@SuppressWarnings("serial")
	public <T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target) {
		if (!(target instanceof DIBeanImpl)) {

			throw new RuntimeException(String
					.format("Dont know how to create provider of %s, as it is not instance of DIBeanImpl.class", target
							.toString()));
		}

		DIBeanImpl<T> t = (DIBeanImpl<T>) target;

		ProviderImpl<T> provider = new ProviderImpl<>(t, helper);

		TypeToken<Provider<T>> providerType = new TypeToken<Provider<T>>() {}
				.where(new TypeParameter<T>() {}, t.getProviderType()
						.wrap());

		ConstantProviderBean<Provider<T>> providerBean = new ConstantProviderBean<>(providerType,
				target.getQualifier());
		providerBean.setProvider(() -> provider);
		t.setProviderBean(providerBean);

		logger.debug("ProviderBean {} created and set for {}", providerBean, target);

		// Add provider bean to results separately
		results.addGeneratedBean(providerBean);

		return providerBean;
	}
	// public <T> Provider<T> createProvider(DIBean<T> bean) {
	// 	logger.debug("Creating provider of {}", bean);
	// 	Provider<T> p = new ProviderImpl<>(bean);
	// 	logger.debug("Provider created for {}", bean);

	// 	((DIBeanImpl<T>) bean).setProvider(p);

	// 	return p;
	// }

	/**
	 * Callback called after all providers are created
	 */
	public void providerCreated() {}

	/**
	 * Callback called when provider of the bean is executed
	 * @param bean the bean whose provider was executed
	 * @param instance the new instance created as a result of the operation
	 */
	public void newInstanceCreated(DIBean<?> bean, Object instance) {
		logger.debug("New instance of {} created {}.", bean, instance);
		CommonFunc.safeAddToMapSet(this.objectMap, bean, new WeakReference<>(instance));
	}

	protected ScanAnnotation createScanAnnotationWithBeanManager(Class<? extends DIBeanManager> mgr) {
		return new ScanAnnotation() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanAnnotation.class;
			}

			@Override
			public Class<?>[] beanManager() {
				return new Class<?>[] { mgr };
			}
		};
	}

	protected ScanInterface createScanInterfaceWithBeanManager(Class<? extends DIBeanManager> mgr) {
		return new ScanInterface() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanInterface.class;
			}

			@Override
			public Class<?>[] beanManager() {
				return new Class<?>[] { mgr };
			}
		};
	}

	protected ScanSubClass createScanSubClassWithBeanManager(Class<? extends DIBeanManager> mgr) {
		return new ScanSubClass() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return ScanSubClass.class;
			}

			@Override
			public Class<?>[] beanManager() {
				return new Class<?>[] { mgr };
			}
		};
	}
}