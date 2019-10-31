package app.saikat.DIManagement.Exceptions;

public class CircularDependencyException extends RuntimeException {

	private static final long serialVersionUID = 453581606368231225L;

	public CircularDependencyException(Class<?> parent, Class<?> child) {
		super(String.format("Circular dependency found while creating %s. %s already dependends on %s",
				child.getSimpleName(), parent.getSimpleName(), child.getSimpleName()));
	}
}