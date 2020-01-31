package app.saikat.DIManagement.Test_6;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import app.saikat.Annotations.DIManagement.ScanAnnotation;
import app.saikat.DIManagement.Impl.BeanManagers.SingletonBeanManager;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ScanAnnotation(beanManager = SingletonBeanManager.class)
public @interface MethodAnnot_3 {
}