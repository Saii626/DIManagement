package app.saikat.DIManagement.Impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.PojoCollections.CommonObjects.Either;

public class DIBeanImpl<T> implements DIBean<T> {

	// Annotations
	private final Class<? extends Annotation> qualifier;
	private final Set<Class<? extends Annotation>> nonQualifiers;
	private boolean isSingleton;

	// The type of object stored in the bean
	private final Either<Constructor<T>, Method> type;
	private Provider<T> provider;

	// List of dependencies of this bean. For methods, first bean is for parentBean.
	private List<DIBeanImpl<?>> dependencies = Collections.emptyList();

	// List of all beanManagers. Called in order
	private Set<Class<? extends DIBeanManager>> beanManagers = new HashSet<>();

	// Used when scanning
	DIBeanImpl(Constructor<T> first, Class<? extends Annotation> second,
			Set<Class<? extends Annotation>> nonQualifierAnnotations, boolean isSingleton) {
		this.type = Either.left(first);
		this.nonQualifiers = nonQualifierAnnotations;
		this.qualifier = second;
		this.isSingleton = isSingleton;
	}

	DIBeanImpl(Method first, Class<? extends Annotation> second,
			Set<Class<? extends Annotation>> nonQualifierAnnotations, boolean isSingleton) {
		this.type = Either.right(first);
		this.nonQualifiers = nonQualifierAnnotations;
		this.qualifier = second;
		this.isSingleton = isSingleton;
	}

	/**
	 * Sets the provider of this bean
	 * @param provider of this bean
	 */
	public void setProvider(Provider<T> provider) {
		this.provider = provider;
	}

	/**
	 * Sets the dependencies of this bean in order
	 * @return dependencies of this bean
	 */
	public List<DIBeanImpl<?>> getDependencies() {
		return dependencies;
	}

	/**
	 * Returns the dependencies of this bean
	 * @param dependencies dependencies of this bean
	 */
	// @SuppressWarnings({"rawtypes", "unchecked"})
	public void setDependencies(List<DIBeanImpl<?>> dependencies) {
		this.dependencies = Collections.unmodifiableList(dependencies);
	}

	/**
	 * Returns the underlying constructor or method
	 * @return the underlying constructor or method
	 */
	public Either<Constructor<T>, Method> get() {
		return type;
	}

	/**
	 * Sets the bean's singleton status
	 * @param singleton if the bean is singleton
	 */
	public void setSingleton(boolean singleton) {
		this.isSingleton = singleton;
	}

	/**
	 * Adds managers for this bean
	 * @param managers managers for this bean
	 */
	public void addManagers(Collection<Class<? extends DIBeanManager>> managers) {
		beanManagers.addAll(managers);
	}

	@Override
	public Class<? extends Annotation> getQualifier() {
		return qualifier;
	}

	@Override
	public Set<Class<? extends Annotation>> getNonQualifierAnnotations() {
		return nonQualifiers;
	}

	@Override
	public Provider<T> getProvider() {
		return provider;
	}

	@Override
	public boolean isSingleton() {
		return isSingleton;
	}
	
	@Override
	public Set<Class<? extends DIBeanManager>> getBeanManagers() {
		return beanManagers;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getProviderType() {
		return type.apply(c -> c.getDeclaringClass(), m -> (Class<T>) m.getReturnType());
	}

	// NonQualifier annotations dont take part in this
	@Override
	public int hashCode() {
		return (qualifier != null ? qualifier.hashCode() : 0) * 31 + type.apply(c -> c.hashCode(), m -> m.hashCode());
	}

	// NonQualifier annotations dont take part in this
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DIBeanImpl)) {
			return false;
		} else {
			DIBeanImpl<?> t = (DIBeanImpl<?>) obj;

			return (qualifier != null ? (t.getQualifier() != null && qualifier.equals(t.getQualifier()))
					: t.getQualifier() == null)
					&& type.apply(c -> t.type.containsLeft() ? t.type.getLeft().get().equals(c) : false,
							m -> t.type.containsRight() ? t.type.getRight().get().equals(m) : false);
		}
	}

	@Override
	public String toString() {
		String qString = qualifier != null ? "(@" + qualifier.getSimpleName() + ") " : "";
		String tString = type.apply(c -> c.getName(),
				m -> m.getDeclaringClass().getSimpleName() + (Modifier.isStatic(m.getModifiers()) ? "." : "::") + m.getName());
		return "<" + qString + tString + ">";
	}

}