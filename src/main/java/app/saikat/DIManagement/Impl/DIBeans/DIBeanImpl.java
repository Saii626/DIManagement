package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.PojoCollections.CommonObjects.Either;

public class DIBeanImpl<T> implements DIBean<T> {

	// Annotations
	protected final Class<? extends Annotation> qualifier;
	protected final Class<? extends Annotation> nonQualifier;
	protected boolean isSingleton;

	// The information stored in the bean
	protected final Either<Constructor<T>, Method> type;
	protected final List<String> genericParameters;
	protected final DIBeanType beanType;
	protected final List<DIBean<?>> dependencies;

	// Other objects required for functioning of the bean
	protected DIBean<Provider<T>> providerBean = null;
	protected DIBeanManager beanManager = null;

	protected DIBeanImpl(Either<Constructor<T>, Method> underlyingVal, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, boolean isSingleton,
			DIBeanType type) {
		this.type = underlyingVal;
		this.nonQualifier = nonQualifierAnnotations;
		this.qualifier = second;
		this.isSingleton = isSingleton;
		this.beanType = type;
		this.genericParameters = new ArrayList<>();
		this.dependencies = new ArrayList<>();
	}

	public DIBeanImpl(Constructor<T> first, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, boolean isSingleton, DIBeanType type) {
		this(Either.left(first), second, nonQualifierAnnotations, isSingleton, type);
	}

	public DIBeanImpl(Method first, Class<? extends Annotation> second,
			Class<? extends Annotation> nonQualifierAnnotations, boolean isSingleton, DIBeanType type) {
		this(Either.right(first), second, nonQualifierAnnotations, isSingleton, type);
	}

	public DIBeanImpl(DIBean<T> bean) {
		this(bean.get(), bean.getQualifier(), bean.getNonQualifierAnnotation(),
				bean.isSingleton(), bean.getBeanType());

		this.beanManager = bean.getBeanManager();
		this.providerBean = ((DIBeanImpl<T>)bean).getProviderBean();
		this.dependencies.addAll(bean.getDependencies());
	}

	/**
	 * Sets the provider of this bean
	 * @param provider of this bean
	 */
	public void setProviderBean(DIBean<Provider<T>> provider) {
		this.providerBean = provider;
	}

	/**
	 * Gets the providerBean of this bean
	 * @return provider bean of this bean
	 */
	public DIBean<Provider<T>> getProviderBean() {
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

	/**
	 * Sets this beans bean manager
	 * @param beanManager the beanManager to set
	 */
	public void setBeanManager(DIBeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Override
	public Either<Constructor<T>, Method> get() {
		return type;
	}

	@Override
	public List<String> getGenericParameters() {
		return genericParameters;
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
		return providerBean.getProvider().get();
	}


	@Override
	public boolean isSingleton() {
		return isSingleton;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getProviderType() {
		return type.apply(c -> c.getDeclaringClass(), m -> (Class<T>) m.getReturnType());
	}

	@Override
	public DIBeanManager getBeanManager() {
		return this.beanManager;
	}

	@Override
	public DIBeanType getBeanType() {
		return this.beanType;
	}

	// NonQualifier annotations dont take part in this
	@Override
	public int hashCode() {
		return type.apply(Constructor::hashCode, Method::hashCode) + 31 * (qualifier != null ? qualifier.hashCode() : 0)
				+ 31 ^ 2 * (genericParameters != null ? genericParameters.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DIBeanImpl) {
			DIBeanImpl<?> t = (DIBeanImpl<?>) obj;
			Class<? extends Annotation> t_qualifier = t.getQualifier();
			Either<?, ?> t_type = t.get();
			List<String> t_genericParam = t.getGenericParameters();

			return (qualifier == null ? t_qualifier == null : qualifier.equals(t_qualifier)) && type.equals(t_type)
					&& genericParameters.equals(t_genericParam);
		}

		return false;
	}

	@Override
	public String toString() {
		String qString = getQualifierString();
		String nqString = getNonQualifierString();
		String gString = getGenericParamString();
		String tString = getTypeString();

		return "[" + qString + ":" + nqString + ":" + tString + gString + "]";
	}

	/**
	 * Only used by toString method to generate string representation of the bean
	 * @return qualifier representation of the string
	 */
	protected String getQualifierString() {
		return qualifier != null ? "@" + qualifier.getSimpleName() : "null";
	}

	/**
	 * Only used by toString method to generate string representation of the bean
	 * @return non qualifier representation of the string
	 */
	protected String getNonQualifierString() {
		return nonQualifier != null ? "@" + nonQualifier.getSimpleName() : "null";
	}

	/**
	 * Only used by toString method to generate string representation of the bean
	 * @return type representation of the string
	 */
	protected String getTypeString() {
		return type.apply(c -> c.getDeclaringClass().getSimpleName(), m -> m.getDeclaringClass().getSimpleName()
				+ (Modifier.isStatic(m.getModifiers()) ? "." : "::") + m.getName());
	}

	/**
	 * Only used by toString method to generate string representation of the bean
	 * @return generic parameter representation of the string
	 */
	protected String getGenericParamString() {
		return genericParameters != null && genericParameters.size() > 0 ? "<" + genericParameters.toString() + ">" : "";
	}
}