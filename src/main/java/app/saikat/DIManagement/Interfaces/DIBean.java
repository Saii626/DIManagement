package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.inject.Provider;

public interface DIBean<T> {

	/**
	 * If the bean has any @Qualifier annotated annotation
	 * @return Qualifier annotated annotaion if present else NoQualifier
	 */
	Class<? extends Annotation> getQualifier();

	/**
	 * Set of non qualifier annotations of the bean
	 * @return Set of non qualifier annotations if present, empty otherwise
	 */
	Set<Class<? extends Annotation>> getNonQualifierAnnotations();

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
	 * Get bean managers registered for this bean
	 * @return all beanManagers for this bean
	 */
	Set<Class<? extends DIBeanManager>> getBeanManagers();
}