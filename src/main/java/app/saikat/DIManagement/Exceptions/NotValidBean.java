package app.saikat.DIManagement.Exceptions;

import app.saikat.DIManagement.Interfaces.DIBean;

public class NotValidBean extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NotValidBean(DIBean<?> bean, String reason) {
		super(String.format("Bean %s not valid. Reason: %s", bean, reason));
	}

}