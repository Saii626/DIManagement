package app.saikat.DIManagement.Exceptions;

public class ObjectAreadyDefinedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ObjectAreadyDefinedException(Class<?> cls) {
        super(String.format("Object already associated with %s", cls.getSimpleName()));
    }

    public ObjectAreadyDefinedException(Class<?> qualifier, Class<?> cls) {
        super(String.format("Object already associated with @%s %s", qualifier.getSimpleName(), cls.getSimpleName()));
    }
}