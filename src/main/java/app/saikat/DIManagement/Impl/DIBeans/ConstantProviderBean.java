package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.inject.Provider;

import app.saikat.DIManagement.Interfaces.DIBeanType;

public class ConstantProviderBean<T> extends DIBeanImpl<T> {

	private Class<T> type;
	private Provider<T> provider;

	@SuppressWarnings("unused")
	private void dummyMethod() {
	}

	public ConstantProviderBean(T object, Class<T> type, Class<? extends Annotation> qualifierAnnotation,
			List<String> genericParams, DIBeanType beanType) {
		super(ConstantProviderBean.class.getDeclaredMethods()[0], qualifierAnnotation, null, false, beanType);

		provider = new Provider<T>() {

			@Override
			public T get() {
				return object;
			}
		};

		this.type = type;
		this.genericParameters.addAll(genericParams);
	}

	@Override
	public Class<T> getProviderType() {
		return this.type;
	}

	@Override
	public Provider<T> getProvider() {
		return this.provider;
	}

	public void setProvider(Provider<T> provider) {
		this.provider = provider;
	}

	@Override
	public String toString() {
		return "c" + super.toString();
	}

	@Override
	protected String getTypeString() {
		return type.getSimpleName();
	}
}