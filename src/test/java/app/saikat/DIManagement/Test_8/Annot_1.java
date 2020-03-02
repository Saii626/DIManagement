package app.saikat.DIManagement.Test_8;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import app.saikat.Annotations.DIManagement.Scan;

@Scan(beanManager = CustomBeanManager.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
public @interface Annot_1 {

}