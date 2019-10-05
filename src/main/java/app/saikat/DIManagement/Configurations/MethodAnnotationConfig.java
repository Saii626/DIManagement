package app.saikat.DIManagement.Configurations;

import java.lang.annotation.Annotation;

public class MethodAnnotationConfig extends AnnotationConfig {

    private final boolean autoInvoke;

    protected MethodAnnotationConfig(Class<? extends Annotation> annotation, boolean autoBuild, boolean checkDependency, boolean autoInvoke) {
        super(annotation, autoBuild, checkDependency);
        this.autoInvoke = autoInvoke;
    }

    public boolean autoInvoke() {
        return autoInvoke;
    }

    public static MethodAnnotationConfigBuilder getBuilder() {
        return new MethodAnnotationConfigBuilder();
    }
    
    public static class MethodAnnotationConfigBuilder extends AnnotationConfig.Builder<MethodAnnotationConfigBuilder> {

        private boolean autoInvoke = false;

        public MethodAnnotationConfigBuilder autoInvoke(boolean autoInvoke) {
            this.autoInvoke = autoInvoke;
            if (this.autoInvoke) autoBuild = true;
            return this;
        } 

        @Override
        public MethodAnnotationConfig build() {
            if (autoInvoke && !autoBuild) {
                throw new RuntimeException("Cannot auto invoke without autoBuild");
            }
            return new MethodAnnotationConfig(annotation, autoBuild, checkDependency, autoInvoke);
        }

    }
}