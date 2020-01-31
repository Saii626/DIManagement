package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;

import app.saikat.DIManagement.Interfaces.DIBeanType;

/**
 * A placeholder for unresolved DIBeanImpl. Internal class. Not to be outside app.saikat.DIManagement
 * @param <T> type of bean
 */
public class UnresolvedDIBeanImpl<T> extends DIBeanImpl<T> {

	private final Class<T> cls;
	private static Constructor<?> dummyInstance = UnresolvedDIBeanImpl.class.getDeclaredConstructors()[0];

	@SuppressWarnings("unchecked")
	public UnresolvedDIBeanImpl(Class<T> cls, Class<? extends Annotation> qualifier, List<String> genericParams) {
		super((Constructor<T>) UnresolvedDIBeanImpl.dummyInstance, qualifier, null, false, DIBeanType.GENERATED);
		this.cls = cls;
		this.genericParameters.addAll(genericParams);
	}

	@Override
	public Class<T> getProviderType() {
		return this.cls;
	}

	@Override
	public String toString() {
		return "u" + super.toString();
	}

	@Override
	protected String getTypeString() {
		return this.cls.getSimpleName();
	}
}