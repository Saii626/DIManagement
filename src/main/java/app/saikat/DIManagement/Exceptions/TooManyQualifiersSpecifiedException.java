package app.saikat.DIManagement.Exceptions;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;

public class TooManyQualifiersSpecifiedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TooManyQualifiersSpecifiedException(ClassInfo cls) {
		super("Too many qualifiers deifned for " + cls.getSimpleName() + " class");
	}

	public TooManyQualifiersSpecifiedException(MethodInfo method) {
		super("Too many qualifiers deifned for " + method.getName() + "(" + method.getClassInfo()
				.getSimpleName() + ") method");
	}
}