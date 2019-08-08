package app.saikat.DIManagement.Exceptions;

public class InsufficientDependency extends RuntimeException {
    private static final long serialVersionUID = 7216412965263440247L;

    public InsufficientDependency(Class<?> parent, Class<?> dependency) {
        super(String.format("No %s found while creating %s", dependency.getSimpleName(), parent.getSimpleName()));
    }
}