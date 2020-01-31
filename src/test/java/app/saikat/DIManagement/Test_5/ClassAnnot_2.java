package app.saikat.DIManagement.Test_5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.DIManagement.Impl.BeanManagers.SingletonBeanManager;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ScanAnnotation(beanManager = SingletonBeanManager.class)
public @interface ClassAnnot_2 {
}