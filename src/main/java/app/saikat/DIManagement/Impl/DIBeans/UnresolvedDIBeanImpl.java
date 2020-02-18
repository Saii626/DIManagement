package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.TypeToken;

import app.saikat.DIManagement.Interfaces.DIBean;
import app.saikat.DIManagement.Interfaces.DIBeanManager;
import app.saikat.DIManagement.Interfaces.DIBeanType;

/**
 * A placeholder for unresolved DIBeanImpl. Internal class. Not to be outside app.saikat.DIManagement
 * @param <T> type of bean
 */
public class UnresolvedDIBeanImpl<T> implements DIBean<T> {

	private final TypeToken<T> typeToken;
	private final Class<? extends Annotation> qualifier;

	public UnresolvedDIBeanImpl(TypeToken<T> type, Class<? extends Annotation> qualifier) {
		this.typeToken = type;
		this.qualifier = qualifier;
	}

	@Override
	public Class<? extends Annotation> getQualifier() {
		return this.qualifier;
	}

	@Override
	public Class<? extends Annotation> getNonQualifierAnnotation() {
		return null;
	}

	@Override
	public Provider<T> getProvider() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public TypeToken<T> getProviderType() {
		return this.typeToken;
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
	public String toString() {

		String qString = qualifier != null ? "@" + qualifier.getSimpleName() : "null";
		String tString = getProviderType().toString();

		return "u[" + qString + ":" + tString + "]";
	}
}