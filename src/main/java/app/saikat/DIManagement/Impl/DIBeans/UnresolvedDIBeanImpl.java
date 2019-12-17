package app.saikat.DIManagement.Impl.DIBeans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

/**
 * A placeholder for unresolved DIBeanImpl. Internal class. Not to be outside app.saikat.DIManagement
 * @param <T> type of bean
 */
public class UnresolvedDIBeanImpl<T> extends DIBeanImpl<T> {

	private final Class<T> cls;
	private static Constructor<?> dummyInstance = UnresolvedDIBeanImpl.class.getDeclaredConstructors()[0];

	@SuppressWarnings("unchecked")
	public UnresolvedDIBeanImpl(Class<T> cls, Class<? extends Annotation> qualifier) {
		super((Constructor<T>) UnresolvedDIBeanImpl.dummyInstance, qualifier, null, false);
		this.cls = cls;
	}

	@Override
	public boolean equals(Object obj) {

		// Should not compare Unresolved dependencies with each other
		if (obj == null || !(obj instanceof DIBeanImpl) || obj instanceof UnresolvedDIBeanImpl) return false;

		return ((DIBeanImpl<?>) obj).getProviderType().equals(this.cls);
	}

	@Override
	public String toString() {
		String qString = qualifier != null ? "(@" + qualifier.getSimpleName() + ") " : "";
		return "u<" + qString + cls.getSimpleName() + ">";
	}

	@Override
	public Class<T> getProviderType() {
		return this.cls;
	}

}