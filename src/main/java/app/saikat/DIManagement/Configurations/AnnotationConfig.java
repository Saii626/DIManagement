package app.saikat.DIManagement.Configurations;

import java.lang.annotation.Annotation;

public abstract class AnnotationConfig {

    private final Class<? extends Annotation> annotation;
    private final boolean autoBuild;
    private final boolean checkDependency;

    protected AnnotationConfig(Class<? extends Annotation> annotation, boolean autoBuild, boolean checkDependency) {
        this.annotation = annotation;
        this.autoBuild = autoBuild;
        this.checkDependency = checkDependency;
    }

    public Class<? extends Annotation> getAnnotation() {
        return this.annotation;
    }

    public boolean isAutoBuild() {
        return this.autoBuild;
    }

    public boolean checkDependency() {
        return this.checkDependency;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().equals(obj.getClass())) {
            AnnotationConfig t = (AnnotationConfig) obj;
            return t.annotation.equals(annotation);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.annotation.hashCode();
    }

    // public static Builder newBuilder();

    public abstract static class Builder<T extends Builder<T>> {
        
        protected Class<? extends Annotation> annotation = null;
        protected boolean autoBuild = false;
        protected boolean checkDependency = false;

        public T forAnnotation(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
            return self();
        }

        public T autoBuild(boolean autoBuild) {
            this.autoBuild = autoBuild;
            return self();
        }

        public T checkDependency(boolean checkDependency) {
            this.checkDependency = checkDependency;
            return self();
        }

        @SuppressWarnings("unchecked")
        private T self() {
            return (T) this;
        }

        public abstract AnnotationConfig build();
        
    }

}