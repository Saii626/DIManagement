package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.Scan;
import app.saikat.PojoCollections.CommonObjects.Either;
import app.saikat.PojoCollections.Utils.CommonFunc;

/*------------------ Imports to impl --------------------*/
import app.saikat.DIManagement.Impl.BeanManagers.InjectBeanManager;
import app.saikat.DIManagement.Impl.BeanManagers.PostConstructBeanManager;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;
import app.saikat.DIManagement.Impl.Repository.Repository;
/*------------------ Imports to impl --------------------*/

public abstract class DIBeanManager {

	protected Logger logger = LogManager.getLogger(this.getClass());

	/**
	 * Object to repository. Initially this repository is localRepo. After successful
	 * merging of the localRepo, this is replaced with globalRepo pointer
	 */
	protected Repository repo = null;

	/**
	 * This pointer is initially this is null. After successful merging of the localRepo,
	 * this is replaced with the actual objcetMap.
	 */
	protected Map<DIBean<?>, Set<WeakReference<?>>> objectMap = null;

	private static Map<Either<DIBean<?>, Class<?>>, Set<WeakReference<DIBeanInvocationListener<?>>>> listenersMap = new ConcurrentHashMap<>();

	public static <T> void addListenerForBean(DIBean<T> bean, DIBeanInvocationListener<T> listener) {
		// Preconditions.checkArgument(bean.getBeanManager()
		// 		.equals(this),
		// 		String.format("Bean %s is not registered for BeanManager %s", bean.toString(), this.toString()));
		CommonFunc.safeAddToMapSet(listenersMap, Either.left(bean), new WeakReference<>(listener));
	}

	public static <T> void addListenerForClass(Class<T> cls, DIBeanInvocationListener<T> listener) {
		CommonFunc.safeAddToMapSet(listenersMap, Either.right(cls), new WeakReference<>(listener));
	}

	/**
	 * Sets the repository
	 * @param repo repository to point to
	 */
	public void setRepo(Repository repo) {
		this.repo = repo;
	}

	public void setObjectMap(Map<DIBean<?>, Set<WeakReference<?>>> objectMap) {
		this.objectMap = objectMap;
	}

	/**
	 * Adds annotations / interfaces / superclasses to scan. This does not mean that
	 * no other bean can have this as their {@link DIBeanManager}. An annotation
	 * can explicitly its DIBeanManager when annotated with {@link Scan}
	 * @return a map of cls and Scan which should be scanned
	 */
	public Map<Class<?>, Scan> addToScan() {
		return Collections.emptyMap();
	}

	/**
	 * Callback called after a bean has been created
	 * @param <T> type of bean created
	 * @param bean bean that has been created
	 */
	public <T> void beanCreated(DIBean<T> bean) {
		this.repo.addBean(bean);
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
		return false;
	}

	/**
	 * Method called to scan and resolve dependencies of the target bean. This also
	 * sets the dependencies of the bean and adds them to dependency graph
	 * @param <T> type of bean
	 * @param target the bean whose dependencies need to be resolved
	 * @param alreadyResolved collection of already resolved beans
	 * @param toBeResolved collection of yet to be resolved beans
	 * @param allQualifiers collection of all qualifier annotaions. Contains qualifiers
	 * from both, the globalRepo and localRepo
	 * @return list of resolved dependencies of the bean
	 */
	public <T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved, Collection<Class<? extends Annotation>> allQualifiers) {
		return null;
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
		return false;
	}

	/**
	 * Method called to create provider bean.
	 * @param <T> type of object returned by provider
	 * @param target the bean whose provider needs to be created
	 * @param injectBeanManager an instance of injectBeanManager. Used to execute setterInjections
	 * @param postConstructBeanManager an instance of postConstructBeanManager. Used to execute
	 * postConstruct
	 * @return providerBean for target bean
	 */
	public <T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target,
			InjectBeanManager injectBeanManager, PostConstructBeanManager postConstructBeanManager) {
		return null;
	}

	/**
	 * Callback called after all providers are created
	 */
	public void providerCreated() {
	}

	/**
	 * Callback called when provider of the bean is executed
	 * @param <T> type of object created
	 * @param bean the bean whose provider was executed
	 * @param instance the new instance created as a result of the operation
	 */
	@SuppressWarnings("unchecked")
	public <T> void newInstanceCreated(DIBean<T> bean, T instance) {
		if (instance == null)
			return; // For void methods

		logger.debug("Instance not null. Invoking listeners");

		CommonFunc.addToMapSet(this.objectMap, bean, new WeakReference<>(instance));
		List<Set<WeakReference<DIBeanInvocationListener<?>>>> setOfListeners = Collections
				.synchronizedList(new ArrayList<>());

		if (listenersMap.containsKey(Either.left(bean))) {
			setOfListeners.add(listenersMap.get(Either.left(bean)));
		}

		Class<T> cls = (Class<T>) instance.getClass();
		listenersMap.entrySet()
				.parallelStream()
				.filter(e -> e.getKey()
						.getRight()
						.isPresent()
						&& e.getKey()
								.getRight()
								.get().isAssignableFrom(cls))
				.forEach(e -> setOfListeners.add(e.getValue()));
		logger.debug("All listeners are: {}", setOfListeners);

		setOfListeners.parallelStream()
				.forEach(set -> {
					synchronized (set) {
						Iterator<WeakReference<DIBeanInvocationListener<?>>> it = set.iterator();

						while (it.hasNext()) {
							DIBeanInvocationListener<T> l = (DIBeanInvocationListener<T>) it.next()
									.get();

							if (l == null) {
								it.remove();
							} else {
								logger.debug("Notifying listener: {}", l);
								l.onObjectCreated(bean, instance);
							}
						}
					}
				});
	}

	/**
	 * Helper method to create a Scan object with current class as BeanManager
	 * @return Scan object with current class as BeanManager
	 */
	protected Scan createScanObject() {
		DIBeanManager current = this;

		return new Scan() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return Scan.class;
			}

			@Override
			public Class<?>[] beanManager() {
				return new Class<?>[] { current.getClass() };
			}
		};
	}
}