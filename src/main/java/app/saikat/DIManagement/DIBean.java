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

    public DIBean(Class<?> first, Class<? extends Annotation> second) {
        this.type = first;
        this.qualifier = second;
        this.method = null;
        this.isMethod = false;
    }

    public DIBean(Method first, Class<? extends Annotation> second) {
        this.type = first.getReturnType();
        this.method = first;
        this.qualifier = second;
        this.isMethod = true;
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

    @Override
    public int hashCode() {
        return (qualifier != null ? qualifier.hashCode() : 0) * 31 + type.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DIBean)) {
            return false;
        } else {
            DIBean t = (DIBean) obj;

            return (qualifier == null ? t.getQualifier() == null : qualifier.equals(t.getQualifier()))
                    && type.equals(t.getType());
        }
    }

    @Override
    public String toString() {
        return "[" + (qualifier != null ? "(@" + qualifier.getSimpleName() + ") " : "") + (isMethod ? "m " : "c ")
                + type.getSimpleName() + "]";
    }
}