package app.saikat.DIManagement.Interfaces;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Provider;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.saikat.PojoCollections.CommonObjects.Copyable;

public interface DIBean<T> extends Copyable<DIBean<T>> {

	/**
	 * Common logger for DIBean
	 */
	Logger logger = LogManager.getLogger(DIBean.class);

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
	 * Superclass of the class of this bean. Only set for {@link DIBeanType#INTERFACE} and {@link DIBeanType#SUBCLASS}
	 * @return the superclass of this bean
	 */
	Class<?> getSuperClass();

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
	TypeToken<T> getProviderType();

	/**
	 * Gets underlying constructor or method
	 * See {@link #getDependencies()} to get the order of dependencies
	 * of the invokable
	 * @return the underlying constructor or method
	 */
	Invokable<Object, T> getInvokable();

	/**
	 * Gets the list of generic type of the bean if present. Else returns empty list
	 * @return generic types of the bean if present, else empty list
	 */
	// List<String> getGenericParameters();

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
	 * Gets the dependency list of this bean. The dependency is the receiver bean. Others are 
	 * parameter beans. Receiver bean is null only for static methods and constructors.
	 * @return dependencies of the bean
	 */
	List<DIBean<?>> getDependencies();
}