package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Impl.DIManagerImpl;
import app.saikat.PojoCollections.CommonObjects.Tuple;

public abstract class DIManager {

	private static DIManager INSTANCE;
	protected static final Logger logger = LogManager.getLogger(DIManager.class);

	/**
	 * Gets the current instance of DIManager
	 * @return current instance of DIManager
	 */
	public static DIManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Creates a new instance of DIManager and returns the instance
	 * @return new instance of DIManager
	 */
	public static DIManager newInstance() {
		INSTANCE = new DIManagerImpl();
		return INSTANCE;
	}

	static {
		newInstance();
	}

	protected Results results;

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
		return results.getAnnotationMap().get(annotation);
	}

	/**
	 * Gets all beans which implement required interface
	 * @param interfaceCls the interface
	 * @return set of beans which impletents the required interface
	 */
	public Set<DIBean<?>> getBeansOfInterface(Class<?> interfaceCls) {
		return results.getInterfacesMap().get(interfaceCls);
	}

	/**
	 * Gets all beans which extends required superclass
	 * @param superClass the superclass
	 * @return set of beans which impletents the required superclass
	 */
	public Set<DIBean<?>> getBeansOfSuperClass(Class<?> superClass) {
		return results.getInterfacesMap().get(superClass);
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
					Set<DIBean<?>> beans = results.getAnnotationBeans().parallelStream()
							.filter(bean -> bean.getNonQualifierAnnotations().contains(annotation))
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
		
		BiFunction<DIBean<?>, Class<? extends Annotation>, Boolean> beanHasAnnotation = (bean, annotation) -> {
			Class<? extends Annotation> q = bean.getQualifier();
			if (q != null && q.equals(annotation)) {
				return true;
			}

			return bean.getNonQualifierAnnotations().contains(annotation);
		};

		if (!cachedBeansMap.containsKey(key)) {
			synchronized (cachedBeansMap) {
				if (!cachedBeansMap.containsKey(key)) {
					Stream<DIBean<?>> annotBeans = results.getAnnotationBeans().parallelStream()
							.filter(bean -> bean.getProviderType().equals(cls) && beanHasAnnotation.apply(bean, annot));
					Stream<DIBean<?>> interfaceBeans = results.getInterfaceBeans().parallelStream()
							.filter(bean -> bean.getProviderType().equals(cls) && beanHasAnnotation.apply(bean, annot));
					Stream<DIBean<?>> superclassBeans = results.getSubclassBeans().parallelStream()
							.filter(bean -> bean.getProviderType().equals(cls) && beanHasAnnotation.apply(bean, annot));

					Set<DIBean<?>> beans = Stream.of(annotBeans, interfaceBeans, superclassBeans).flatMap(s -> s)
							.collect(Collectors.toSet());
					cachedBeansMap.put(key, beans);
					logger.debug("Added {} in cache", beans);
				}
			}
		}

		return (Set) cachedBeansMap.get(key);
	}

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
	public abstract Map<DIBean<?>, Set<WeakReference<?>>> getImmutableObjectMap();
}