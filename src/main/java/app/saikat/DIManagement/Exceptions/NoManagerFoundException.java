package app.saikat.DIManagement.Exceptions;

public class NoManagerFoundException extends Exception {

	private static final long serialVersionUID = 1L;


	public NoManagerFoundException(Class<?> cls) {
		super(String.format("No manager found for bean of type %s", cls.getName()));

	}

}