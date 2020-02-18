package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.DIManagerImpl;
import app.saikat.PojoCollections.CommonObjects.Tuple;

public abstract class DIManager {

	protected final Logger logger = LogManager.getLogger(DIManager.class);

	/**
	 * Returns a new current instance of DIManager
	 * @return current instance of DIManager
	 */
	public static DIManager newInstance() {
		return new DIManagerImpl();
	}

	protected final Results results = new Results();
	protected final Map<DIBean<?>, Set<WeakReference<?>>> objectMap = new ConcurrentHashMap<>();

	/**
	 * Performs classpath scanning, generation of dependency graph and creation of
	 * providers  on classes found in pathsToScan
	 * @param pathsToScan paths to scan
	 */
	public abstract void initialize(String... pathsToScan);

	protected void makeResultImmutable() {
		results.makeImmutable();
	}

	/**
	 * Get result of scanning
	 * @return scan result
	 */
	public Results getResult() {
		return results;
	}

	/**
	 * Gets all beans with a particular Qualifier annotation
	 * @param annotation the Qualifier annotation
	 * @return set of beans which have the required qualifier annotation
	 */
	public Set<DIBean<?>> getBeansWithQualifierAnnotation(Class<? extends Annotation> annotation) {
		return results.getAnnotationMap()
				.get(annotation);
	}

	/**
	 * Gets all beans which implement required interface
	 * @param interfaceCls the interface
	 * @return set of beans which impletents the required interface
	 */
	public Set<DIBean<?>> getBeansOfInterface(Class<?> interfaceCls) {
		return results.getInterfacesMap()
				.get(interfaceCls);
	}

	/**
	 * Gets all beans which extends required superclass
	 * @param superClass the superclass
	 * @return set of beans which impletents the required superclass
	 */
	public Set<DIBean<?>> getBeansOfSuperClass(Class<?> superClass) {
		return results.getSubClassesMap()
				.get(superClass);
	}

	// Cache results of annotaions
	private Map<Class<? extends Annotation>, Set<DIBean<?>>> cachedAnnotationMap = new ConcurrentHashMap<>();

	/**
	 * Finds beans annotated with specified annotation. Result is cached
	 * @param annotation annotation searched for
	 * @return set of beans with specified annotation
	 */
	public Set<DIBean<?>> getBeansAnnotatedWith(Class<? extends Annotation> annotation) {

		if (!cachedAnnotationMap.containsKey(annotation)) {
			synchronized (cachedAnnotationMap) {
				if (!cachedAnnotationMap.containsKey(annotation)) {
					Set<DIBean<?>> beans = results.getAnnotationBeans()
							.parallelStream()
							.filter(bean -> bean.getNonQualifierAnnotation()
									.equals(annotation))
							.collect(Collectors.toSet());

					cachedAnnotationMap.put(annotation, beans);
				}
			}
		}

		return cachedAnnotationMap.get(annotation);
	}

	// Cache beans type
	private final Map<Tuple<Class<?>, Class<? extends Annotation>>, Set<DIBean<?>>> cachedBeansMap = new ConcurrentHashMap<>();

	/**
	 * Returns set of beans which provides specified type of object and qualifier annotation
	 * @param <T> type of object
	 * @param cls class type of object required
	 * @param annot qualifier of the object
	 * @return set of beans which satisfies the condition
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Set<DIBean<T>> getBeansOfType(Class<T> cls, Class<? extends Annotation> annot) {
		Tuple<Class<?>, Class<? extends Annotation>> key = Tuple.of(cls, annot);

		if (!cachedBeansMap.containsKey(key)) {
			synchronized (cachedBeansMap) {
				if (!cachedBeansMap.containsKey(key)) {
					Set<DIBean<T>> beans = getBeanOfTypeUncached(cls, annot);
					logger.debug("Added {} in cache", beans);
					cachedBeansMap.put(key, (Set) beans);
				}
			}
		}

		return (Set) cachedBeansMap.get(key);
	}

	protected abstract <T> Set<DIBean<T>> getBeanOfTypeUncached(Class<T> cls, Class<? extends Annotation> annot);

	/**
	 * Returns set of beans which provides specified type of object and has no qualifier annotation
	 * @param <T> type of object
	 * @param cls class type of object required
	 * @return set of beans which satisfies the condition
	 */
	public <T> Set<DIBean<T>> getBeansOfType(Class<T> cls) {
		return getBeansOfType(cls, NoQualifier.class);
	}

	/**
	 * Returns immutable view of object map. Underlying map may change anytime, and
	 * trying to synchronize on the returned map won't be useful
	 * @return an immutable view of object map (map of DIBean to set of objects created by provider of the bean)
	 */
	public Map<DIBean<?>, Set<WeakReference<?>>> getObjectMap() {
		return this.objectMap;
	}

	/**
	 * Special case when there is only 1 instance of type
	 * @param <T> class type
	 * @param cls class whose bean is required
	 * @return returns the unique bean associated with this class
	 * @throws RuntimeException if no or multiple beans are found of this class type
	 */
	public <T> DIBean<T> getBeanOfType(Class<T> cls) {
		return getBeanOfType(cls, NoQualifier.class);
	}

	/**
	 * Special case when there is only 1 instance of type
	 * @param <T> class type
	 * @param cls class whose bean is required
	 * @param qualifier qualifier
	 * @return returns the unique bean associated with this class
	 * @throws RuntimeException if no or multiple beans are found of this class type
	 */
	public <T> DIBean<T> getBeanOfType(Class<T> cls, Class<? extends Annotation> qualifier) {
		Set<DIBean<T>> beans = getBeansOfType(cls, qualifier);

		if (beans.size() == 1) {
			return beans.iterator()
					.next();
		} else {
			throw new RuntimeException("No unique bean for " + cls.getSimpleName() + " found");
		}
	}
}