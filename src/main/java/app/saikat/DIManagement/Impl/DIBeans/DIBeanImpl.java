package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;

public class DIBeanImpl<T> implements DIBean<T> {

	// Annotations
	private final Class<? extends Annotation> qualifier;
	private final Class<? extends Annotation> nonQualifier;
	private final Class<?> superClass;
	private boolean isSingleton;

	// The information stored in the bean
	private final Invokable<Object, T> invokable;
	private final boolean isMethod;
	private final DIBeanType beanType;
	private final List<DIBean<?>> dependencies;

	// @SuppressWarnings("serial")
	private final TypeToken<T> providerType;

	// Other objects required for functioning of the bean
	private final DIBeanManager beanManager;

	private ConstantProviderBean<Provider<T>> providerBean;

	@SuppressWarnings({ "unchecked" })
	private DIBeanImpl(Invokable<Object, T> underlyingInvokable, boolean isMethod, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, Class<?> superclass, boolean isSingleton,
			DIBeanManager beanManager, DIBeanType type) {

		Preconditions.checkNotNull(second, "Qualifier annotation cannot be null");
		Preconditions.checkNotNull(beanManager, "BeanManager cannot be null");
		Preconditions.checkNotNull(type, "Bean type cannot be null");

		this.invokable = underlyingInvokable;
		this.nonQualifier = nonQualifierAnnotations;
		this.qualifier = second;
		this.isSingleton = isSingleton;
		this.beanType = type;
		this.beanManager = beanManager;
		this.isMethod = isMethod;
		this.superClass = superclass;

		this.providerType = (TypeToken<T>) invokable.getReturnType();
		this.dependencies = new ArrayList<>();
	}

	public DIBeanImpl(DIBeanImpl<T> bean) {
		this(bean.getInvokable(), bean.isMethod(), bean.getQualifier(), bean.getNonQualifierAnnotation(),
				bean.getSuperClass(), bean.isSingleton(), bean.getBeanManager(), bean.getBeanType());

		this.dependencies.addAll(bean.getDependencies());
		this.providerBean = bean.getProviderBean() != null ? bean.getProviderBean()
				.copy() : null;
	}

	@SuppressWarnings("unchecked")
	public DIBeanImpl(Constructor<T> first, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, Class<?> superClass, boolean isSingleton,
			DIBeanManager beanManager, DIBeanType type) {
		this((Invokable<Object, T>) Invokable.from(first), false, second, nonQualifierAnnotations, superClass,
				isSingleton, beanManager, type);
	}

	@SuppressWarnings("unchecked")
	public DIBeanImpl(Method first, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, Class<?> superClass, boolean isSingleton,
			DIBeanManager beanManager, DIBeanType type) {
		this((Invokable<Object, T>) Invokable.from(first), true, second, nonQualifierAnnotations, superClass,
				isSingleton, beanManager, type);
	}

	@Override
	public DIBeanImpl<T> copy() {
		return new DIBeanImpl<>(this);
	}

	public void setProviderBean(ConstantProviderBean<Provider<T>> providerBean) {
		this.providerBean = providerBean;
	}

	/**
	 * Gets the providerBean of this bean
	 * @return provider bean of this bean
	 */
	public ConstantProviderBean<Provider<T>> getProviderBean() {
		return this.providerBean;
	}

	/**
	 * Sets the dependencies of this bean in order
	 * @return dependencies of this bean
	 */
	public List<DIBean<?>> getDependencies() {
		return dependencies;
	}

	/**
	 * Sets the bean's singleton status
	 * @param singleton if the bean is singleton
	 */
	public void setSingleton(boolean singleton) {
		this.isSingleton = singleton;
	}

	@Override
	public Invokable<Object, T> getInvokable() {
		return invokable;
	}

	public boolean isMethod() {
		return this.isMethod;
	}

	@Override
	public Class<? extends Annotation> getQualifier() {
		return qualifier;
	}

	@Override
	public Class<? extends Annotation> getNonQualifierAnnotation() {
		return nonQualifier;
	}

	@Override
	public Provider<T> getProvider() {
		return providerBean.getProvider()
				.get();
	}

	@Override
	public boolean isSingleton() {
		return isSingleton;
	}

	@Override
	public TypeToken<T> getProviderType() {
		return this.providerType;
	}

	@Override
	public DIBeanManager getBeanManager() {
		return this.beanManager;
	}

	@Override
	public DIBeanType getBeanType() {
		return this.beanType;
	}

	@Override
	public Class<?> getSuperClass() {
		return this.superClass;
	}

	// NonQualifier annotations dont take part in this
	@Override
	public int hashCode() {
		return (invokable != null ? invokable.hashCode() : 0) + 31 * (qualifier != null ? qualifier.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DIBeanImpl) {
			DIBean<?> t = (DIBean<?>) obj;
			Class<? extends Annotation> t_qualifier = t.getQualifier();
			Invokable<?, ?> t_type = t.getInvokable();

			return (qualifier == null ? t_qualifier == null : qualifier.equals(t_qualifier))
					&& (invokable == null ? t_type == null : invokable.equals(t_type));
		}

		return false;
	}

	@Override
	public String toString() {
		String qString = qualifier != null ? "@" + qualifier.getSimpleName() : "null";
		String nqString = nonQualifier != null ? "@" + nonQualifier.getSimpleName() : "null";
		String tString = getProviderType().toString();
		String methodName = this.invokable.getName();

		return "[" + qString + ":" + nqString + ":" + methodName + "(" + tString + ")]";
	}
}