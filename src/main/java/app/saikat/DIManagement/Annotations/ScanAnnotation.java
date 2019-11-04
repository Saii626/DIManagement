package app.saikat.DIManagement.Annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta annotation.
 * Annotations annotated with this will be scanned by DIManager
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScanAnnotation {

	/** 
	 * If true, will attempt to auto build the object. If annotatation is found on class, setting it to true
	 * will automatically build as part of DIManager initialization. If annotation is found on method, setting
	 * it to true will automatically build the declaring class as a part of DIManager initialization
	*/
	boolean autoBuild() default false;

	/**
	 * If true will perform a recursive search for this dependency. If annotated to class, setting it to true
	 * adds constructor arguments as dependency and continues recursively searching for dependencies. If annotated
	 * to method, setting it to true will add its arguments as dependency and recursively searches for dependencies.
	 * If method is non static, then declaring class is also added as dependency
	 */
	boolean checkDependency() default false;

	/**
	 * Only used when annotated to methods. If set to true, will auto invoke the method as a part of DIManager
	 * initialization
	 */
	boolean autoInvoke() default false;
}