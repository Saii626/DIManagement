package app.saikat.DIManagement.Configurations;

import java.lang.annotation.Annotation;

public class ClassAnnotationConfig extends AnnotationConfig {

    protected ClassAnnotationConfig(Class<? extends Annotation> annotation, boolean autoBuild, boolean checkDependency) {
        super(annotation, autoBuild, checkDependency);
    }

    public static ClassAnnotationConfigBuilder getBuilder() {
        return new ClassAnnotationConfigBuilder();
    }

    public static class ClassAnnotationConfigBuilder extends AnnotationConfig.Builder<ClassAnnotationConfigBuilder> {

        @Override
        public ClassAnnotationConfig build() {
            return new ClassAnnotationConfig(annotation, autoBuild, checkDependency);
        }
    }
}