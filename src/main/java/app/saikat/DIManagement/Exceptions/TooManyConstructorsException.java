package app.saikat.DIManagement.Exceptions;

public class TooManyConstructorsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TooManyConstructorsException(Class<?> cls) {
        super(String.format("No unique constructor found for %s", cls.getName()));
    }
}