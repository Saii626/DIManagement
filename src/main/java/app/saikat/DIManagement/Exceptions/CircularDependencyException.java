package app.saikat.DIManagement.Exceptions;

import app.saikat.DIManagement.Interfaces.DIBean;

public class CircularDependencyException extends RuntimeException {

	private static final long serialVersionUID = 453581606368231225L;

	private DIBean<?> target, dependent;

	public CircularDependencyException(DIBean<?> target, DIBean<?> dependent) {
		super(String.format("Circular dependency found while scanning %s. %s already dependends on %s",
				dependent, target, dependent));

		this.target = target;
		this.dependent = dependent;
	}

	public DIBean<?> getTarget() {
		return target;
	}

	public DIBean<?> getDependent() {
		return dependent;
	}
}