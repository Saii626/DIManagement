package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.Annotations.DIManagement.ScanInterface;
import app.saikat.Annotations.DIManagement.ScanSubClass;
import app.saikat.DIManagement.Impl.DIBeans.ConstantProviderBean;

public interface DIBeanManager {

	Logger logger = LogManager.getLogger(DIBeanManager.class);

	/**
	 * Adds an annotation to scan. This does not mean that no other bean can
	 * have this as their {@link DIBeanManager}. An annotation can explicitly
	 * its DIBeanManager when annotated with {@link ScanAnnotation}
	 * @return a map of annotation and scanSnnotation whose beans should be
	 * maintained by this class
	 */
	Map<Class<? extends Annotation>, ScanAnnotation> addAnnotationsToScan();

	/**
	 * Adds an interface to scan. This does not mean that no other bean can
	 * have this as their {@link DIBeanManager}. An annotation can explicitly
	 * its DIBeanManager when annotated with {@link ScanInterface}
	 * @return a map of interface and scanInterface whose beans should be
	 * maintained by this class
	 */
	Map<Class<?>, ScanInterface> addInterfacessToScan();

	/**
	 * Adds an superclass to scan. This does not mean that no other bean can
	 * have this as their {@link DIBeanManager}. An annotation can explicitly
	 * its DIBeanManager when annotated with {@link ScanSubClass}
	 * @return a map of superclass and scanSubClass whose beans should be
	 * maintained by this class
	 */
	Map<Class<?>, ScanSubClass> addSubClassesToScan();

	/**
	 * Callback called after a bean has been created
	 * @param <T> type of bean created
	 * @param bean bean that has been created
	 */
	<T> void beanCreated(DIBean<T> bean);

	/**
	 * Callback called after all scan has been done. Create additional beans in this
	 * if necessary
	 */
	void scanComplete();

	/**
	 * If dependenies of the bean scould be scanned and resolved
	 * @return true if the bean should be scanned and resolved, else false
	 */
	boolean shouldResolveDependency();

	/**
	 * Method called to scan and resolve dependencies of the target bean. This also
	 * sets the dependencies of the bean and adds them to dependency graph
	 * @param <T> type of bean
	 * @param target the bean whose dependencies need to be resolved
	 * @param alreadyResolved collection of already resolved beans
	 * @param toBeResolved collection of yet to be resolved beans
	 * @return list of resolved dependencies of the bean
	 */
	<T> List<DIBean<?>> resolveDependencies(DIBean<T> target, Collection<DIBean<?>> alreadyResolved,
			Collection<DIBean<?>> toBeResolved);

	/**
	 * Callback called after all dependencies of beans have been resolved
	 */
	void dependencyResolved();

	/**
	 * If provider of the bean scould be scanned and resolved
	 * @return true if the bean should be scanned and resolved, else false
	 */
	boolean shouldCreateProvider();

	/**
	 * Method called to create provider bean.
	 * @param <T> type of object returned by provider
	 * @param target the bean whose provider needs to be created
	 * @return providerBean for target bean
	 */
	<T> ConstantProviderBean<Provider<T>> createProviderBean(DIBean<T> target);

	/**
	 * Callback called after all providers are created
	 */
	void providerCreated();

	/**
	 * Callback called when provider of the bean is executed
	 * @param bean the bean whose provider was executed
	 * @param instance the new instance created as a result of the operation
	 */
	void newInstanceCreated(DIBean<?> bean, Object instance);
}