package app.saikat.DIManagement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

class DIBean {
    private final Class<?> cls;
    private final Method method;
    private final Class<? extends Annotation> qualifier;
    private final boolean isMethod;
    private final boolean isProvider;

    public DIBean(Class<?> first, Class<? extends Annotation> second, boolean isProvider) {
        this.cls = first;
        this.qualifier = second;
        this.method = null;
        this.isMethod = false;
        this.isProvider = isProvider;
    }

    public DIBean(Method first, Class<? extends Annotation> second, boolean isProvider) {
        this.method = first;
        this.qualifier = second;
        this.cls = null;
        this.isMethod = true;
        this.isProvider = isProvider;
    }

    public DIBean(Class<?> first, Class<? extends Annotation> second) {
        this(first, second, false);
    }

    public DIBean(Method first, Class<? extends Annotation> second) {
        this(first, second, false);
    }

    public Class<?> getType() {
        return this.cls;
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

    public boolean isClass() {
        return !this.isMethod;
    }

    public boolean isProvider() {
        return isProvider;
    }
    
    public Class<?> provides() {
        return isMethod ? method.getReturnType() : cls;
    }

    @Override
    public int hashCode() {
        return (qualifier != null ? qualifier.hashCode() : 0) * 31 + (isMethod ? method.getReturnType().hashCode() : cls.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof DIBean)) {
            return false;
        } else {
            DIBean t = (DIBean) obj;

            boolean a = qualifier == null ? t.getQualifier() == null : qualifier.equals(t.getQualifier());
            if (!a) return false;

            Class<?> t1 = isMethod ? method.getReturnType() : cls;
            Class<?> t2 = t.isMethod() ? t.getMethod().getReturnType() : t.getType();
            return t1.equals(t2);
        }
    }

    @Override
    public String toString() {
        return "[" + (qualifier != null ? "(@" + qualifier.getSimpleName() + ") " : "")
                + (isMethod ? ("m "+method.getReturnType().getSimpleName()) : "c "+cls.getSimpleName()) + "]";
    }
}