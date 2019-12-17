package app.saikat.DIManagement.Test_2;

import javax.inject.Inject;

public class B {

	private D d;
	private E e;

	@Inject
	public B(D d, E e) {
		this.d = d;
		this.e = e;
	}

	public D getD() {
		return this.d;
	}

	public E getE() {
		return this.e;
	}

}