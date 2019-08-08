package app.saikat.DIManagement.Exceptions;

public class ClassNotUnderDIException extends RuntimeException {

	private static final long serialVersionUID = 1L;

    public ClassNotUnderDIException(Class<?> cls) {
        super(String.format("%s not scanned by DI. No annotations found", cls.getName()));
    }
}