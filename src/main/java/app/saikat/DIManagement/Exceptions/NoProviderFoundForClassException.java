package app.saikat.DIManagement.Exceptions;

public class NoProviderFoundForClassException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoProviderFoundForClassException(Class<?> cls) {
		super(String.format("No provider found for class %s", cls.getSimpleName()));
	}

}