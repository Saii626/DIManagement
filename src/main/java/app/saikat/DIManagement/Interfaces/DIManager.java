package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Exceptions.BeanNotFoundException;

/*------------------ Imports to impl --------------------*/
import app.saikat.DIManagement.Impl.DIManagerImpl;
import app.saikat.DIManagement.Impl.Repository.Repository;
/*------------------ Imports to impl --------------------*/

public abstract class DIManager {

	/**
	 * Returns a new current instance of DIManager
	 * @return current instance of DIManager
	 */
	public static DIManager newInstance() {
		return new DIManagerImpl();
	}

	protected final Repository repository = new Repository();
	protected final Map<DIBean<?>, Set<WeakReference<?>>> objectMap = new ConcurrentHashMap<>();
	protected final Logger logger = LogManager.getLogger(DIManager.class);

	/**
	 * Performs classpath scanning, generation of dependency graph and creation of
	 * providers on classes found in pathsToScan. On successful, adds them to repository 
	 * @param pathsToScan paths to scan
	 */
	public abstract void scan(String... pathsToScan);

	/**
	 * Returns a set of beans with specific type. This is the non qualifier, interface or
	 * superclass of the bean
	 * @param type the non qualifier / interface / superclass
	 * @return set of beans which satisfies the condition
	 */
	public Set<DIBean<?>> getBeansWithType(Class<?> type) {
		return this.repository.getBeans()
				.parallelStream()
				.filter(b -> {
					switch (b.getBeanType()) {
					case ANNOTATION:
						return b.getNonQualifierAnnotation()
								.equals(type);
					case INTERFACE:
					case SUBCLASS:
						return b.getSuperClass()
								.equals(type);
					default:
						return false;
					}
				})
				.collect(Collectors.toSet());
	}

	/**
	 * Returns set of beans which provides specified type of object and qualifier annotation
	 * @param <T> type of object
	 * @param typeToken typeToken of provider, i.e. type of object this bean generates
	 * @param annot qualifier of the object
	 * @return set of beans which satisfies the condition
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Set<DIBean<T>> getBeansOfType(TypeToken<T> typeToken, Class<? extends Annotation> annot) {
		return (Set) this.repository.getBeans()
				.parallelStream()
				.filter(b -> b.getProviderType()
						.isSubtypeOf(typeToken)
						&& b.getQualifier()
								.equals(annot))
				.collect(Collectors.toSet());
	}

	public <T> Set<DIBean<T>> getBeansOfType(Class<T> cls, Class<? extends Annotation> annot) {
		return getBeansOfType(TypeToken.of(cls), annot);
	}


	/**
	 * Special case when there is only 1 instance of type
	 * @param <T> class type
	 * @param type typetoken of whose bean is required
	 * @param qualifier qualifier
	 * @return returns the unique bean associated with this class
	 * @throws BeanNotFoundException if no or multiple beans are found of this class type
	 */
	public <T> DIBean<T> getBeanOfType(TypeToken<T> type, Class<? extends Annotation> qualifier)
			throws BeanNotFoundException {
		Set<DIBean<T>> beans = getBeansOfType(type, qualifier);

		if (beans.size() == 1) {
			return beans.iterator()
					.next();
		} else {
			throw new BeanNotFoundException(type, qualifier);
		}
	}


	public <T> DIBean<T> getBeanOfType(Class<T> type, Class<? extends Annotation> qualifier)
			throws BeanNotFoundException {
		return getBeanOfType(TypeToken.of(type), qualifier);
	}

	/**
	 * Returns set of beans which provides specified type of object and has no qualifier annotation
	 * @param <T> type of object
	 * @param type type of object required
	 * @return set of beans which satisfies the condition
	 */
	public <T> Set<DIBean<T>> getBeansOfType(TypeToken<T> type) {
		return getBeansOfType(type, NoQualifier.class);
	}


	public <T> Set<DIBean<T>> getBeansOfType(Class<T> type) {
		return getBeansOfType(TypeToken.of(type));
	}

	/**
	 * Returns all beans which have the specified qualifie annotation
	 * @param qualifierAnnotation the qualifier annotation to search for
	 * @return set of beans which have the specified qualifier annotation
	 */
	public Set<DIBean<?>> getBeansWithQualifier(Class<? extends Annotation> qualifierAnnotation) {
		return repository.getBeans()
				.parallelStream()
				.filter(b -> qualifierAnnotation.equals(b.getQualifier()))
				.collect(Collectors.toSet());
	}

	/**
	 * Special case when there is only 1 instance of type
	 * @param <T> class type
	 * @param cls class whose bean is required
	 * @return returns the unique bean associated with this class
	 * @throws BeanNotFoundException if no or multiple beans are found of this class type
	 */
	public <T> DIBean<T> getBeanOfType(TypeToken<T> cls) throws BeanNotFoundException {
		return getBeanOfType(cls, NoQualifier.class);
	}

	public <T> DIBean<T> getBeanOfType(Class<T> cls) throws BeanNotFoundException {
		return getBeanOfType(TypeToken.of(cls));
	}
	

	/**
	 * Returns immutable view of object map. Underlying map may change anytime, and
	 * trying to synchronize on the returned map won't be useful
	 * @return an immutable view of object map (map of DIBean to set of objects created by provider of the bean)
	 */
	public Map<DIBean<?>, Set<WeakReference<?>>> getObjectMap() {
		return this.objectMap;
	}
}