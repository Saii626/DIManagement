package app.saikat.DIManagement.Test_1;

import javax.inject.Inject;

public class D {

	private E e;

	@Inject
	public D(E e) {
		this.e = e;
	}

	public E getE() {
		return e;
	}
}