package app.saikat.DIManagement.Exceptions;

public class WrongGenericParams extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WrongGenericParams(String str){
		super(str);
	}
}