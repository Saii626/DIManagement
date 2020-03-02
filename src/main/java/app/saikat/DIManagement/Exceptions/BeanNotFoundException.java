package app.saikat.DIManagement.Exceptions;

import java.lang.annotation.Annotation;

import com.google.common.reflect.TypeToken;

public class BeanNotFoundException extends Exception {

	private static final long serialVersionUID = 1L;

	public BeanNotFoundException(TypeToken<?> token, Class<? extends Annotation> qualifier) {
		super(String.format("No beans with provider type %s and qualifier %s found", token, qualifier.getSimpleName()));
	}
}