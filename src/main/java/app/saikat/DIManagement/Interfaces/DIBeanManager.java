package app.saikat.DIManagement.Interfaces;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.DIManagement.Exceptions.CircularDependencyException;
import app.saikat.DIManagement.Impl.DIBeanManagerHelper;
import app.saikat.DIManagement.Impl.DependencyHelper;
import app.saikat.PojoCollections.Utils.CommonFunc;

public class DIBeanManager {

	protected final Results results;
	protected final DIBeanManagerHelper helper;
	protected final MutableGraph<DIBean<?>> mutableGraph;
	protected final Map<DIBean<?>, Set<WeakReference<?>>> objectMap;

	protected Logger logger = LogManager.getLogger(this.getClass());

	public DIBeanManager(Results results, MutableGraph<DIBean<?>> mutableGraph, 
			Map<DIBean<?>, Set<WeakReference<?>>> objectMap, DIBeanManagerHelper helper) {
		this.results = results;
		this.mutableGraph = mutableGraph;
		this.objectMap = objectMap;
		this.helper = helper;
	}

	/**
	 * Callback called after a bean has been created
	 * @param bean bean that has been created
	 * @param type Can be null for annotation beans. For interface and superclass beans
	 * 		type refers to the interface and superclass respectively 
	 */
	public void beanCreated(DIBean<?> bean, Class<?> type) {
		switch (bean.getBeanType()) {
		case ANNOTATION:
			results.addAnnotationBean(bean);
			break;
		case INTERFACE:
			results.addInterfaceBean(bean, type);
			break;
		case SUBCLASS:
			results.addSubclassBean(bean, type);
			break;
		case GENERATED:
			results.addGeneratedBean(bean);
		}
	}

	/**
	 * Callback called after all scan has been done. Create additional beans in this
	 * if necessary
	 */
	public void scanComplete() {
	}

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
	public List<DIBean<?>> resolveDependencies(DIBean<?> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved) {

		logger.debug("Scanning dependencies of {}", target);
		List<DIBean<?>> unresolvedDependencies = DependencyHelper.scanAndSetDependencies(target, results);
		logger.debug("Unresolved dependencies of {}: {}", target, unresolvedDependencies);
		List<DIBean<?>> resolvedDependencies = DependencyHelper.resolveAndSetDependencies(target, alreadyResolved,
				toBeResolved);
		logger.debug("Resolved dependencies of {}: {}", target, resolvedDependencies);

		resolvedDependencies.forEach(dep -> checkAndAddPair(target, dep));
		mutableGraph.addNode(target);

		return resolvedDependencies;
	}

	/**
	 * Callback called after all dependencies of beans have been resolved
	 */
	public void dependencyResolved() {
	}

	/**
	 * If provider of the bean scould be scanned and resolved
	 * @return true if the bean should be scanned and resolved, else false
	 */
	public boolean shouldCreateProvider() {
		return true;
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
	public void providerCreated() {
	}

	/**
	 * Callback called when provider of the bean is executed
	 * @param bean the bean whose provider was executed
	 * @param instance the new instance created as a result of the operation
	 */
	public void newInstanceCreated(DIBean<?> bean, Object instance) {
		logger.debug("New instance of {} created {}.", bean, instance);
		CommonFunc.safeAddToMapSet(this.objectMap, bean, new WeakReference<>(instance));
	}

	/**
	 * Checks and adds dependency to dependency graph
	 * @param target the bean whose dependency is to be captured
	 * @param dependent the bean which is dependent
	 */
	protected void checkAndAddPair(DIBean<?> target, DIBean<?> dependent) {
		if (dependent == null)
			return;
		try {
			logger.trace("Dependency {} -> {} captured", target, dependent);
			mutableGraph.putEdge(target, dependent);

			if (Graphs.hasCycle(mutableGraph)) {
				throw new CircularDependencyException(target, dependent);
			}
		} catch (IllegalArgumentException e) {
			throw new CircularDependencyException(target, dependent);
		}
	}
}