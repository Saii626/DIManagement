package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

class DIBean {

	// Qualifier class
	private final Class<? extends Annotation> qualifier;

	// The type of object stored in the bean
	private final Class<?> type;

	// If generated form method
	private final boolean isMethod;
	private final Method method;
	private final boolean resolveDependency;

	// Used when scanning
	public DIBean(Class<?> first, Class<? extends Annotation> second, boolean resolveDependency) {
		this.type = first;
		this.qualifier = second;
		this.method = null;
		this.isMethod = false;
		this.resolveDependency = resolveDependency;
	}

	// Used when scanning
	public DIBean(Method first, Class<? extends Annotation> second, boolean resolveDependency) {
		this.type = first.getReturnType();
		this.method = first;
		this.qualifier = second;
		this.isMethod = true;
		this.resolveDependency = resolveDependency;
	}

	// Used when querying
	public DIBean(Class<?> first, Class<? extends Annotation> second) {
		this.type = first;
		this.qualifier = second;
		this.method = null;
		this.isMethod = false;
		this.resolveDependency = false;
	}

	// Used when querying
	public DIBean(Method first, Class<? extends Annotation> second) {
		this.type = first.getReturnType();
		this.method = first;
		this.qualifier = second;
		this.isMethod = true;
		this.resolveDependency = false;
	}

	/**
	 *  Used to create Array of particular type from collection. <collection>.toArray(new DIBean())
		Serves no other purpose
	 */
	DIBean() {
		this.type = null;
		this.qualifier = null;
		this.method = null;
		this.isMethod = false;
		this.resolveDependency = false;
	}

	/**
	 * Returns the type of object this bean holds
	 * @return class of the object
	 */
	public Class<?> getType() {
		return this.type;
	}

	public Class<? extends Annotation> getQualifier() {
		return this.qualifier;
	}

	public Method getMethod() {
		return method;
	}

	public boolean isMethod() {
		return isMethod;
	}

	public boolean resolveDependency() {
		return this.resolveDependency;
	}

	@Override
	public int hashCode() {
		// If a method has void return type, use method to generate hashcode, else use return type.
		int code;
		if (isMethod && type.equals(Void.class)) {
			code = method.hashCode();
		} else {
			code = type.hashCode();
		}
		return (qualifier != null ? qualifier.hashCode() : 0) * 31 + code;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof DIBean)) {
			return false;
		} else {
			DIBean t = (DIBean) obj;

			// If methods has void return type, compare the methods directly
			if (isMethod && type.equals(Void.class) && t.isMethod() && t.getType()
					.equals(Void.class)) {
				return (qualifier == null ? t.getQualifier() == null : qualifier.equals(t.getQualifier()))
						&& method.equals(t.getMethod());
			} else {
				return (qualifier == null ? t.getQualifier() == null : qualifier.equals(t.getQualifier()))
						&& type.equals(t.getType());
			}
		}
	}

	@Override
	public String toString() {
		return "[" + (qualifier != null ? "(@" + qualifier.getSimpleName() + ") " : "")
				+ (isMethod ? "m " + method.getName() : "c") + " " + type.getSimpleName() + "]";
	}
}