package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Provider;

import app.saikat.PojoCollections.CommonObjects.Either;

public interface DIBean<T> {

	/**
	 * If the bean has any @Qualifier annotated annotation
	 * @return Qualifier annotated annotaion if present else NoQualifier
	 */
	Class<? extends Annotation> getQualifier();

	/**
	 * Non qualifier annotation of the bean
	 * @return Non qualifier annotation if present, empty otherwise
	 */
	Class<? extends Annotation> getNonQualifierAnnotation();

	/**
	 * Returns a provider to create new instance or invoke a function. There is exactly one
	 * Provider of a particular type that is shared across the application
	 * @return a provider for this bean
	 */
	Provider<T> getProvider();

	/**
	 * Indicates if the bean is singleton. If true, only 1 instance of the bean will be created
	 * and providers of this bean will return same object when "get"ed. If false, each invocation
	 * of "get" on this bean's providers will results in creation of new object.
	 * @return true if the bean is singleton
	 */
	boolean isSingleton();

	/**
	 * Returns runtime class information of the bean. Provider of this type is returned for managed objects
	 * @return runtime class information of the bean
	 */
	Class<T> getProviderType();


	/**
	 * Gets underlying constructor or method
	 * @return the underlying constructor or method
	 */
	Either<Constructor<T>, Method> get();

	/**
	 * Gets the list of generic type of the bean if present. Else returns empty list
	 * @return generic types of the bean if present, else empty list
	 */
	List<String> getGenericParameters();

	/**
	 * Get bean manager for this bean
	 * @return beanManager for this bean
	 */
	DIBeanManager getBeanManager();


	/**
	 * Returns the type of bean
	 * @return type of bean
	 */
	DIBeanType getBeanType();


	/**
	 * Gets the dependency list of this bean
	 * @return dependencies of the bean
	 */
	List<DIBean<?>> getDependencies();
}