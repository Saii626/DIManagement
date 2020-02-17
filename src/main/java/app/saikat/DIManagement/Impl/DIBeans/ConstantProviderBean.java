package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIBeanType;
import app.saikat.PojoCollections.CommonObjects.Copyable;
import app.saikat.Annotations.DIManagement.NoQualifier;
import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;

public class ConstantProviderBean<T> implements DIBean<T>, Copyable<ConstantProviderBean<T>> {

	private final Class<? extends Annotation> qualifier;
	private final TypeToken<T> type;

	private Provider<T> provider;

	// @SuppressWarnings("serial")
	// public ConstantProviderBean(T object, Class<? extends Annotation> qualifierAnnotation) {
	// 	Preconditions.checkNotNull(qualifierAnnotation, "Qualifier annotation cannot be null");
	// 	Preconditions.checkNotNull(object, "Cannot be a null provider");

	// 	this.provider = () -> object;
	// 	this.qualifier = qualifierAnnotation;
	// 	this.type = new TypeToken<T>(getClass()) {};
	// }

	public ConstantProviderBean(TypeToken<T> type, Class<? extends Annotation> qualifierAnnotation) {
		Preconditions.checkNotNull(qualifierAnnotation, "Qualifier annotation cannot be null");
		Preconditions.checkNotNull(type, "Type cannot be null");

		this.type = type;
		this.qualifier = qualifierAnnotation;
		this.provider = null;
	}

	@Override
	public ConstantProviderBean<T> copy() {
		ConstantProviderBean<T> newCopy = new ConstantProviderBean<>(this.type, this.qualifier);
		newCopy.setProvider(this.provider);
		return newCopy;
	}

	public void setProvider(Provider<T> provider) {
		this.provider = provider;
	}

	@Override
	public Class<? extends Annotation> getQualifier() {
		return this.qualifier;
	}

	@Override
	public Class<? extends Annotation> getNonQualifierAnnotation() {
		return NoQualifier.class;
	}

	@Override
	public Provider<T> getProvider() {
		return this.provider;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public TypeToken<T> getProviderType() {
		return this.type;
	}

	@Override
	public Invokable<Object, T> getInvokable() {
		return null;
	}

	@Override
	public DIBeanManager getBeanManager() {
		return null;
	}

	@Override
	public Class<?> getSuperClass() {
		return null;
	}
	
	@Override
	public DIBeanType getBeanType() {
		return DIBeanType.GENERATED;
	}

	@Override
	public List<DIBean<?>> getDependencies() {
		return Collections.emptyList();
	}

	@Override
	public int hashCode() {
		return type.hashCode() + 31 * (qualifier != null ? qualifier.hashCode() : 0);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ConstantProviderBean) {
			DIBean<?> t = (DIBean<?>) obj;
			Class<? extends Annotation> t_qualifier = t.getQualifier();
			Invokable<?, ?> t_invokable = t.getInvokable();

			return (qualifier == null ? t_qualifier == null : qualifier.equals(t_qualifier))
					&& t_invokable == null;
		}

		return false;
	}

	@Override
	public String toString() {
		String qString = qualifier != null ? "@" + qualifier.getSimpleName() : "null";
		String tString = getProviderType().toString();

		return "c[" + qString  + ":" + tString + "]";
	}
}