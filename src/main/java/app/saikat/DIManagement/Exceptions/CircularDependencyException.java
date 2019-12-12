package app.saikat.DIManagement.Exceptions;

import app.saikat.DIManagement.Interfaces.DIBean;

public class CircularDependencyException extends RuntimeException {

	private static final long serialVersionUID = 453581606368231225L;

	public CircularDependencyException(DIBean<?> target, DIBean<?> dependent) {
		super(String.format("Circular dependency found while scanning %s. %s already dependends on %s",
				dependent, target, dependent));
	}
}