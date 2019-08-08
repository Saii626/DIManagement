package app.saikat.DIManagement.Exceptions;

public class NoValidConstructorFoundException extends RuntimeException {

    private static final long serialVersionUID = 2475836232500687074L;

    public NoValidConstructorFoundException(Class<?> cls) {
        super(String.format("No @Inject annotated or default constructor found for %s class ", cls.getSimpleName()));
    }
}