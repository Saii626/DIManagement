package app.saikat.DIManagement.Exceptions;

public class WrongGeneratorParamsProvided extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WrongGeneratorParamsProvided(String str) {
		super(str);
	}

}